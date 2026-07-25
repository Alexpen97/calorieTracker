import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  disconnectSamsungHealth,
  fetchSamsungHealthStatus,
  type SamsungHealthStatus,
} from '../../api/client'
import {
  collectAndSyncSamsungHealth,
  getConnectionState,
  isSamsungHealthFeatureEnabled,
  isSamsungHealthSupported,
} from '../../platform/samsungHealth'
import { SettingsSectionShell } from './SettingsSectionShell'

export default function SettingsIntegrationsSection() {
  const queryClient = useQueryClient()
  const [actionError, setActionError] = useState<string | null>(null)
  const featureEnabled = isSamsungHealthFeatureEnabled()
  const supported = isSamsungHealthSupported()

  const statusQuery = useQuery({
    queryKey: ['samsung-health-status'],
    queryFn: fetchSamsungHealthStatus,
    enabled: featureEnabled,
  })

  const connectionQuery = useQuery({
    queryKey: ['samsung-health-connection'],
    queryFn: getConnectionState,
    enabled: featureEnabled,
  })

  const syncMutation = useMutation({
    mutationFn: () => collectAndSyncSamsungHealth({ daysBack: 7 }),
    onSuccess: async () => {
      setActionError(null)
      await queryClient.invalidateQueries({ queryKey: ['samsung-health-status'] })
      await queryClient.invalidateQueries({ queryKey: ['samsung-health-connection'] })
      await queryClient.invalidateQueries({ queryKey: ['diary-summary'] })
    },
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Sync failed')
    },
  })

  const disconnectMutation = useMutation({
    mutationFn: disconnectSamsungHealth,
    onSuccess: async () => {
      setActionError(null)
      await queryClient.invalidateQueries({ queryKey: ['samsung-health-status'] })
      await queryClient.invalidateQueries({ queryKey: ['samsung-health-connection'] })
      await queryClient.invalidateQueries({ queryKey: ['diary-summary'] })
    },
    onError: (error) => {
      setActionError(error instanceof Error ? error.message : 'Disconnect failed')
    },
  })

  const status = statusQuery.data
  const connection = connectionQuery.data
  const syncing = syncMutation.isPending
  const viewState = resolveViewState({
    featureEnabled,
    supported,
    status,
    connectionStatus: connection?.status,
    syncing,
  })

  return (
    <SettingsSectionShell
      title="Integrations"
      description="Connect health providers to adjust daily calorie targets from activity."
    >
      {!featureEnabled && <p>Health integrations are currently disabled.</p>}

      {featureEnabled && (statusQuery.isLoading || connectionQuery.isLoading) && <p>Loading…</p>}
      {featureEnabled && statusQuery.error && (
        <p className="error">{(statusQuery.error as Error).message}</p>
      )}
      {actionError && <p className="error">{actionError}</p>}

      {featureEnabled && (
        <section className="dashboard-card" aria-label="Samsung Health">
          <h3>Samsung Health</h3>
          <p className="product-meta">{viewState.description}</p>
          {viewState.lastSyncedLabel ? (
            <p className="product-meta">Last synced {viewState.lastSyncedLabel}</p>
          ) : null}

          <div className="cta-row">
            {viewState.showConnect ? (
              <button
                className="btn"
                type="button"
                disabled={syncing || disconnectMutation.isPending}
                onClick={() => {
                  setActionError(null)
                  syncMutation.mutate()
                }}
              >
                {syncing ? 'Syncing…' : 'Connect'}
              </button>
            ) : null}
            {viewState.showSync ? (
              <button
                className="btn"
                type="button"
                disabled={syncing || disconnectMutation.isPending}
                onClick={() => {
                  setActionError(null)
                  syncMutation.mutate()
                }}
              >
                {syncing ? 'Syncing…' : 'Sync now'}
              </button>
            ) : null}
            {viewState.showDisconnect ? (
              <button
                className="btn btn-secondary"
                type="button"
                disabled={syncing || disconnectMutation.isPending}
                onClick={() => {
                  setActionError(null)
                  disconnectMutation.mutate()
                }}
              >
                Disconnect
              </button>
            ) : null}
          </div>
        </section>
      )}
    </SettingsSectionShell>
  )
}

function resolveViewState(input: {
  featureEnabled: boolean
  supported: boolean
  status: SamsungHealthStatus | undefined
  connectionStatus: string | undefined
  syncing: boolean
}): {
  description: string
  lastSyncedLabel: string | null
  showConnect: boolean
  showSync: boolean
  showDisconnect: boolean
} {
  if (!input.featureEnabled) {
    return {
      description: 'Samsung Health is disabled.',
      lastSyncedLabel: null,
      showConnect: false,
      showSync: false,
      showDisconnect: false,
    }
  }

  if (input.syncing) {
    return {
      description: 'Syncing activity from Samsung Health…',
      lastSyncedLabel: formatSyncedAt(input.status?.lastSyncedAt ?? null),
      showConnect: false,
      showSync: false,
      showDisconnect: false,
    }
  }

  if (!input.supported || input.connectionStatus === 'unsupported') {
    return {
      description: 'Samsung Health sync requires the Android app.',
      lastSyncedLabel: null,
      showConnect: false,
      showSync: false,
      showDisconnect: false,
    }
  }

  if (input.connectionStatus === 'unavailable') {
    return {
      description: 'Samsung Health is unavailable on this device.',
      lastSyncedLabel: null,
      showConnect: false,
      showSync: false,
      showDisconnect: false,
    }
  }

  const permissionState = input.status?.permissionState?.toUpperCase() ?? ''
  if (
    input.connectionStatus === 'permission_denied' ||
    permissionState === 'DENIED' ||
    permissionState === 'PERMISSION_DENIED'
  ) {
    return {
      description: 'Permission required. Allow Samsung Health access to sync burned calories.',
      lastSyncedLabel: formatSyncedAt(input.status?.lastSyncedAt ?? null),
      showConnect: true,
      showSync: false,
      showDisconnect: Boolean(input.status?.connected),
    }
  }

  if (input.status?.connected) {
    return {
      description: 'Connected. Burned calories adjust today’s calorie target.',
      lastSyncedLabel: formatSyncedAt(input.status.lastSyncedAt),
      showConnect: false,
      showSync: true,
      showDisconnect: true,
    }
  }

  return {
    description: 'Not connected. Connect to sync recent activity and adjust calorie targets.',
    lastSyncedLabel: null,
    showConnect: true,
    showSync: false,
    showDisconnect: false,
  }
}

function formatSyncedAt(value: string | null): string | null {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
