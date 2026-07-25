package com.nutritrack.app;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Capacitor bridge for Samsung Health burned calories (Exercise.CALORIE).
 *
 * Live reads require:
 * 1) Samsung Health partner / app approval for data access, and
 * 2) the Samsung Health SDK AAR on the app compile + runtime classpath
 *    (e.g. app/libs). Without the AAR this plugin still compiles and reports
 *    sdkLinked=false via reflection detection.
 */
@CapacitorPlugin(name = "SamsungHealth")
public class SamsungHealthPlugin extends Plugin {

    private static final String TAG = "SamsungHealth";

    private static final String CLS_HEALTH_DATA_STORE =
            "com.samsung.android.sdk.healthdata.HealthDataStore";
    private static final String CLS_CONNECTION_LISTENER =
            "com.samsung.android.sdk.healthdata.HealthDataStore$ConnectionListener";
    private static final String CLS_PERMISSION_MANAGER =
            "com.samsung.android.sdk.healthdata.HealthPermissionManager";
    private static final String CLS_PERMISSION_KEY =
            "com.samsung.android.sdk.healthdata.HealthPermissionManager$PermissionKey";
    private static final String CLS_PERMISSION_TYPE =
            "com.samsung.android.sdk.healthdata.HealthPermissionManager$PermissionType";
    private static final String CLS_HEALTH_DATA_RESOLVER =
            "com.samsung.android.sdk.healthdata.HealthDataResolver";
    private static final String CLS_READ_REQUEST =
            "com.samsung.android.sdk.healthdata.HealthDataResolver$ReadRequest";
    private static final String CLS_READ_REQUEST_BUILDER =
            "com.samsung.android.sdk.healthdata.HealthDataResolver$ReadRequest$Builder";
    private static final String CLS_RESULT_LISTENER =
            "com.samsung.android.sdk.healthdata.HealthResultHolder$ResultListener";
    private static final String CLS_EXERCISE =
            "com.samsung.android.sdk.healthdata.HealthConstants$Exercise";

    private static final String STATE_SDK_NOT_LINKED = "SDK_NOT_LINKED";
    private static final String STATE_NOT_DETERMINED = "NOT_DETERMINED";
    private static final String STATE_GRANTED = "GRANTED";
    private static final String STATE_DENIED = "DENIED";
    private static final String STATE_UNAVAILABLE = "UNAVAILABLE";

    private volatile String lastPermissionState = STATE_NOT_DETERMINED;

    private static boolean sdkLinkedCached;
    private static boolean sdkChecked;

    /** Background work only — never await Samsung Health latches on the UI thread. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "samsung-health-plugin");
        t.setDaemon(true);
        return t;
    });

    private static synchronized boolean detectSdkLinked() {
        if (sdkChecked) {
            return sdkLinkedCached;
        }
        sdkChecked = true;
        try {
            Class.forName(CLS_HEALTH_DATA_STORE);
            Class.forName(CLS_PERMISSION_MANAGER);
            Class.forName(CLS_HEALTH_DATA_RESOLVER);
            Class.forName(CLS_EXERCISE);
            sdkLinkedCached = true;
        } catch (ClassNotFoundException e) {
            sdkLinkedCached = false;
        }
        return sdkLinkedCached;
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        boolean linked = detectSdkLinked();
        JSObject ret = new JSObject();
        ret.put("supported", true);
        ret.put("sdkLinked", linked);
        if (!linked) {
            lastPermissionState = STATE_SDK_NOT_LINKED;
            ret.put("permissionState", STATE_SDK_NOT_LINKED);
            ret.put(
                    "message",
                    "Samsung Health SDK AAR is not on the classpath. "
                            + "Add the partner-approved SDK AAR to compile/runtime to enable live reads.");
        } else {
            if (STATE_SDK_NOT_LINKED.equals(lastPermissionState)) {
                lastPermissionState = STATE_NOT_DETERMINED;
            }
            ret.put("permissionState", lastPermissionState);
            ret.put(
                    "message",
                    "Samsung Health SDK detected. Partner approval and user consent "
                            + "are required before Exercise.CALORIE reads succeed.");
        }
        call.resolve(ret);
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        if (!detectSdkLinked()) {
            lastPermissionState = STATE_SDK_NOT_LINKED;
            JSObject ret = new JSObject();
            ret.put("granted", false);
            ret.put("permissionState", STATE_SDK_NOT_LINKED);
            call.resolve(ret);
            return;
        }

        final Activity activity = getActivity();
        executor.execute(
                () -> {
                    try {
                        boolean granted = withConnectedStore(
                                store -> {
                                    ensureExerciseReadPermission(store, activity, true);
                                    return isExerciseReadGranted(store);
                                });
                        lastPermissionState = granted ? STATE_GRANTED : STATE_DENIED;
                        JSObject ret = new JSObject();
                        ret.put("granted", granted);
                        ret.put("permissionState", lastPermissionState);
                        call.resolve(ret);
                    } catch (Exception e) {
                        Log.w(TAG, "requestPermissions failed", e);
                        lastPermissionState = STATE_UNAVAILABLE;
                        call.reject(
                                "Samsung Health permission request failed: " + e.getMessage(),
                                "UNAVAILABLE",
                                e);
                    }
                });
    }

    @PluginMethod
    public void readDailyBurns(PluginCall call) {
        String fromDate = call.getString("fromDate");
        String toDate = call.getString("toDate");
        String zone = call.getString("zone");
        if (fromDate == null || toDate == null || zone == null) {
            call.reject("fromDate, toDate, and zone are required", "INVALID_ARGS");
            return;
        }

        if (!detectSdkLinked()) {
            lastPermissionState = STATE_SDK_NOT_LINKED;
            call.reject(
                    "Samsung Health SDK AAR is not linked; cannot read burned calories.",
                    STATE_SDK_NOT_LINKED);
            return;
        }

        final Activity activity = getActivity();
        executor.execute(
                () -> {
                    try {
                        TimeZone tz = TimeZone.getTimeZone(zone);
                        long startMs = startOfLocalDayMillis(fromDate, tz);
                        long endExclusiveMs = startOfLocalDayMillis(toDate, tz) + TimeUnit.DAYS.toMillis(1);
                        if (endExclusiveMs <= startMs) {
                            call.reject("toDate must be on or after fromDate", "INVALID_ARGS");
                            return;
                        }

                        Map<String, DayAgg> days =
                                withConnectedStore(
                                        store -> {
                                            if (!isExerciseReadGranted(store)) {
                                                ensureExerciseReadPermission(store, activity, true);
                                            }
                                            if (!isExerciseReadGranted(store)) {
                                                lastPermissionState = STATE_DENIED;
                                                throw new PermissionDeniedException(
                                                        "Samsung Health exercise read permission not granted");
                                            }
                                            lastPermissionState = STATE_GRANTED;
                                            return readExerciseCalories(
                                                    store, startMs, endExclusiveMs, tz, fromDate, toDate);
                                        });

                        JSObject ret = new JSObject();
                        JSArray dayArr = new JSArray();
                        for (DayAgg day : days.values()) {
                            JSObject row = new JSObject();
                            row.put("localDate", day.localDate);
                            row.put("activeEnergyKcal", day.activeEnergyKcal);
                            row.put("totalEnergyKcal", day.totalEnergyKcal);
                            row.put("selectedBurnKcal", day.selectedBurnKcal);
                            row.put("sourceRecordCount", day.sourceRecordCount);
                            dayArr.put(row);
                        }
                        ret.put("days", dayArr);
                        call.resolve(ret);
                    } catch (PermissionDeniedException e) {
                        call.reject(e.getMessage(), STATE_DENIED);
                    } catch (Exception e) {
                        Log.w(TAG, "readDailyBurns failed", e);
                        lastPermissionState = STATE_UNAVAILABLE;
                        call.reject(
                                "Samsung Health read failed: " + e.getMessage(),
                                "UNAVAILABLE",
                                e);
                    }
                });
    }

    private interface StoreWork<T> {
        T run(Object store) throws Exception;
    }

    private static final class PermissionDeniedException extends Exception {
        PermissionDeniedException(String message) {
            super(message);
        }
    }

    private static final class DayAgg {
        final String localDate;
        double activeEnergyKcal;
        double totalEnergyKcal;
        double selectedBurnKcal;
        int sourceRecordCount;

        DayAgg(String localDate) {
            this.localDate = localDate;
        }
    }

    private <T> T withConnectedStore(StoreWork<T> work) throws Exception {
        Context context = getContext().getApplicationContext();
        Class<?> storeClass = Class.forName(CLS_HEALTH_DATA_STORE);
        Class<?> listenerClass = Class.forName(CLS_CONNECTION_LISTENER);

        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<Object> errorRef = new AtomicReference<>();
        AtomicBoolean ok = new AtomicBoolean(false);

        InvocationHandler handler =
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("onConnected".equals(name)) {
                        ok.set(true);
                        connected.countDown();
                    } else if ("onConnectionFailed".equals(name)) {
                        errorRef.set(args != null && args.length > 0 ? args[0] : "connection failed");
                        connected.countDown();
                    } else if ("onDisconnected".equals(name)) {
                        // no-op
                    }
                    return defaultProxyReturn(method);
                };

        Object listener = Proxy.newProxyInstance(
                listenerClass.getClassLoader(), new Class<?>[] {listenerClass}, handler);

        Constructor<?> ctor = storeClass.getConstructor(Context.class, listenerClass);
        Object store = ctor.newInstance(context, listener);

        Method connect = storeClass.getMethod("connectService");
        Method disconnect = storeClass.getMethod("disconnectService");
        connect.invoke(store);

        if (!connected.await(30, TimeUnit.SECONDS)) {
            try {
                disconnect.invoke(store);
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Timed out connecting to Samsung Health");
        }
        if (!ok.get()) {
            try {
                disconnect.invoke(store);
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Samsung Health connection failed: " + errorRef.get());
        }

        try {
            return work.run(store);
        } finally {
            try {
                disconnect.invoke(store);
            } catch (Exception e) {
                Log.w(TAG, "disconnectService failed", e);
            }
        }
    }

    private boolean isExerciseReadGranted(Object store) throws Exception {
        Object pms = newPermissionManager(store);
        Set<Object> keys = exerciseReadKeys();
        Method isAcquired = pms.getClass().getMethod("isPermissionAcquired", Set.class);
        @SuppressWarnings("unchecked")
        Map<Object, Boolean> map = (Map<Object, Boolean>) isAcquired.invoke(pms, keys);
        if (map == null || map.isEmpty()) {
            return false;
        }
        for (Boolean v : map.values()) {
            if (v == null || !v) {
                return false;
            }
        }
        return true;
    }

    private void ensureExerciseReadPermission(Object store, Activity activity, boolean prompt)
            throws Exception {
        if (isExerciseReadGranted(store)) {
            return;
        }
        if (!prompt || activity == null) {
            return;
        }

        Object pms = newPermissionManager(store);
        Set<Object> keys = exerciseReadKeys();
        Method request =
                pms.getClass().getMethod("requestPermissions", Set.class, Activity.class);
        Object holder = request.invoke(pms, keys, activity);

        CountDownLatch done = new CountDownLatch(1);
        Class<?> resultListenerClass = Class.forName(CLS_RESULT_LISTENER);
        InvocationHandler handler =
                (proxy, method, args) -> {
                    if ("onResult".equals(method.getName())) {
                        done.countDown();
                    }
                    return defaultProxyReturn(method);
                };
        Object listener = Proxy.newProxyInstance(
                resultListenerClass.getClassLoader(),
                new Class<?>[] {resultListenerClass},
                handler);
        Method setListener = holder.getClass().getMethod("setResultListener", resultListenerClass);
        setListener.invoke(holder, listener);
        if (!done.await(120, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for Samsung Health permission UI");
        }
    }

    private Object newPermissionManager(Object store) throws Exception {
        Class<?> pmsClass = Class.forName(CLS_PERMISSION_MANAGER);
        Constructor<?> ctor = pmsClass.getConstructor(Class.forName(CLS_HEALTH_DATA_STORE));
        return ctor.newInstance(store);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<Object> exerciseReadKeys() throws Exception {
        Class<?> exercise = Class.forName(CLS_EXERCISE);
        String dataType = (String) exercise.getField("HEALTH_DATA_TYPE").get(null);

        Class<?> keyClass = Class.forName(CLS_PERMISSION_KEY);
        Class<?> typeClass = Class.forName(CLS_PERMISSION_TYPE);
        Object readType = Enum.valueOf((Class) typeClass, "READ");
        Constructor<?> keyCtor = keyClass.getConstructor(String.class, typeClass);
        Object key = keyCtor.newInstance(dataType, readType);

        Set<Object> keys = new HashSet<>();
        keys.add(key);
        return keys;
    }

    private Map<String, DayAgg> readExerciseCalories(
            Object store,
            long startMs,
            long endExclusiveMs,
            TimeZone tz,
            String fromDate,
            String toDate)
            throws Exception {
        Class<?> exercise = Class.forName(CLS_EXERCISE);
        String dataType = (String) exercise.getField("HEALTH_DATA_TYPE").get(null);
        String calorieField = (String) exercise.getField("CALORIE").get(null);
        String startField = (String) exercise.getField("START_TIME").get(null);
        String timeOffsetField = (String) exercise.getField("TIME_OFFSET").get(null);

        Class<?> resolverClass = Class.forName(CLS_HEALTH_DATA_RESOLVER);
        Constructor<?> resolverCtor =
                resolverClass.getConstructor(Class.forName(CLS_HEALTH_DATA_STORE), android.os.Handler.class);
        Object resolver = resolverCtor.newInstance(store, null);

        Class<?> builderClass = Class.forName(CLS_READ_REQUEST_BUILDER);
        Object builder = builderClass.getConstructor().newInstance();
        builderClass.getMethod("setDataType", String.class).invoke(builder, dataType);
        builderClass
                .getMethod("setLocalTimeRange", String.class, String.class, long.class, long.class)
                .invoke(builder, startField, timeOffsetField, startMs, endExclusiveMs);
        Object request = builderClass.getMethod("build").invoke(builder);

        Method read = resolverClass.getMethod("read", Class.forName(CLS_READ_REQUEST));
        Object holder = read.invoke(resolver, request);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Object> resultRef = new AtomicReference<>();

        Class<?> resultListenerClass = Class.forName(CLS_RESULT_LISTENER);
        InvocationHandler handler =
                (proxy, method, args) -> {
                    if ("onResult".equals(method.getName())) {
                        resultRef.set(args != null && args.length > 0 ? args[0] : null);
                        done.countDown();
                    }
                    return defaultProxyReturn(method);
                };
        Object listener = Proxy.newProxyInstance(
                resultListenerClass.getClassLoader(),
                new Class<?>[] {resultListenerClass},
                handler);
        Method setListener = holder.getClass().getMethod("setResultListener", resultListenerClass);
        setListener.invoke(holder, listener);

        if (!done.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out reading Samsung Health exercise data");
        }

        Map<String, DayAgg> byDate = emptyDaysInclusive(fromDate, toDate);

        Object result = resultRef.get();
        if (result == null) {
            return byDate;
        }

        Iterator<?> iterator;
        if (result instanceof Iterable) {
            iterator = ((Iterable<?>) result).iterator();
        } else {
            Method iteratorMethod = result.getClass().getMethod("iterator");
            iterator = (Iterator<?>) iteratorMethod.invoke(result);
        }

        while (iterator.hasNext()) {
            Object data = iterator.next();
            double kcal = readNumber(data, calorieField);
            long startTime = ((Number) invokeGetter(data, "getLong", startField)).longValue();
            String localDate = formatLocalDate(startTime, tz);
            if (!byDate.containsKey(localDate)) {
                continue;
            }
            DayAgg agg = byDate.get(localDate);
            // Exercise.CALORIE is burned kcal; prefer it as selectedBurnKcal.
            agg.activeEnergyKcal += kcal;
            agg.totalEnergyKcal += kcal;
            agg.selectedBurnKcal += kcal;
            agg.sourceRecordCount += 1;
        }

        try {
            Method close = result.getClass().getMethod("close");
            close.invoke(result);
        } catch (NoSuchMethodException ignored) {
        }

        return byDate;
    }

    private static Map<String, DayAgg> emptyDaysInclusive(String fromDate, String toDate)
            throws ParseException {
        Map<String, DayAgg> byDate = new TreeMap<>();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dayFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date from = dayFmt.parse(fromDate);
        Date to = dayFmt.parse(toDate);
        if (from == null || to == null) {
            throw new ParseException("Invalid local date", 0);
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(from);
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        end.setTime(to);
        while (!cal.after(end)) {
            String key = dayFmt.format(cal.getTime());
            byDate.put(key, new DayAgg(key));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return byDate;
    }

    private static long startOfLocalDayMillis(String localDate, TimeZone tz) throws ParseException {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        fmt.setTimeZone(tz);
        Date parsed = fmt.parse(localDate);
        if (parsed == null) {
            throw new ParseException("Invalid local date: " + localDate, 0);
        }
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(parsed);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static String formatLocalDate(long epochMillis, TimeZone tz) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        fmt.setTimeZone(tz);
        return fmt.format(new Date(epochMillis));
    }

    private static double readNumber(Object data, String field) throws Exception {
        try {
            Object v = invokeGetter(data, "getFloat", field);
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
        } catch (Exception ignored) {
        }
        try {
            Object v = invokeGetter(data, "getDouble", field);
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
        } catch (Exception ignored) {
        }
        Object v = invokeGetter(data, "getInt", field);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return 0d;
    }

    private static Object invokeGetter(Object data, String methodName, String field) throws Exception {
        Method m = data.getClass().getMethod(methodName, String.class);
        return m.invoke(data, field);
    }

    private static Object defaultProxyReturn(Method method) {
        Class<?> rt = method.getReturnType();
        if (!rt.isPrimitive() || rt == void.class) {
            return null;
        }
        if (rt == boolean.class) {
            return false;
        }
        if (rt == byte.class) {
            return (byte) 0;
        }
        if (rt == short.class) {
            return (short) 0;
        }
        if (rt == int.class) {
            return 0;
        }
        if (rt == long.class) {
            return 0L;
        }
        if (rt == float.class) {
            return 0f;
        }
        if (rt == double.class) {
            return 0d;
        }
        if (rt == char.class) {
            return (char) 0;
        }
        return null;
    }
}
