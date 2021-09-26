package com.wisnu.kurniawan.composetodolist.features.todo.taskreminder.ui

import com.wisnu.kurniawan.composetodolist.features.todo.taskreminder.data.ITaskReminderEnvironment
import com.wisnu.kurniawan.composetodolist.foundation.extension.getNextScheduledDueDate
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class TaskReminderViewModel @Inject constructor(
    private val environment: ITaskReminderEnvironment,
    private val alarmManager: TaskAlarmManager,
    private val notificationManager: TaskNotificationManager
) {

    fun dispatch(action: TaskReminderAction) {
        when (action) {
            is TaskReminderAction.AlarmShow -> {
                GlobalScope.launch(environment.dispatcher) {
                    environment.getTask(action.taskId)
                        .collect { (task, listId) ->
                            notificationManager.show(task, listId)
                        }
                }
            }
            TaskReminderAction.AppBootCompleted -> {
                GlobalScope.launch(environment.dispatcher) {
                    environment.getTasksWithDueDate()
                        .collect { tasks ->
                            tasks.forEach {
                                alarmManager.scheduleTaskAlarm(it, it.getNextScheduledDueDate(environment.dateTimeProvider.now()))
                            }
                        }
                }
            }
            is TaskReminderAction.NotificationCompleted -> {
                GlobalScope.launch(environment.dispatcher) {
                    environment.toggleTaskStatus(action.taskId)
                        .collect { (task, _) ->
                            alarmManager.cancelTaskAlarm(task)
                            notificationManager.dismiss(task)
                        }
                }
            }
            is TaskReminderAction.NotificationSnooze -> {
                GlobalScope.launch(environment.dispatcher) {
                    environment.getTask(action.taskId)
                        .collect { (task, _) ->
                            alarmManager.scheduleTaskAlarm(task, environment.dateTimeProvider.now().plusMinutes(15))
                            notificationManager.dismiss(task)
                        }
                }
            }
        }
    }

}
