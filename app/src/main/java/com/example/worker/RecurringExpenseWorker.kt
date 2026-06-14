package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.ExpenseEntity
import java.util.Calendar

class RecurringExpenseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val recurringDao = database.recurringExpenseDao()
        val expenseDao = database.expenseDao()

        try {
            val activeRecurring = recurringDao.getActiveRecurringExpensesDirect()
            val now = System.currentTimeMillis()

            for (item in activeRecurring) {
                if (item.nextDueDate <= now) {
                    var updatedDueDate = item.nextDueDate
                    while (updatedDueDate <= now) {
                        val newExpense = ExpenseEntity(
                            profileId = item.profileId,
                            itemName = item.itemName,
                            price = item.amount,
                            currency = "৳",
                            category = item.category,
                            date = updatedDueDate
                        )
                        expenseDao.insertExpense(newExpense)

                        // Advance the next due date based on frequency
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = updatedDueDate
                        }
                        when (item.frequency) {
                            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
                            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        updatedDueDate = calendar.timeInMillis
                    }

                    // Update next due date in DB
                    val updatedItem = item.copy(nextDueDate = updatedDueDate)
                    recurringDao.updateRecurringExpense(updatedItem)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
