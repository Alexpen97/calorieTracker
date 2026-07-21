package com.nutritrack.diary.client;

import java.util.List;

public interface UserGoalsClient {
  List<UserGoalResponse> getGoals(String bearerToken);
}
