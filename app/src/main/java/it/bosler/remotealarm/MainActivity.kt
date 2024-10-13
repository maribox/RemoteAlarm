package it.bosler.remotealarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {

   /* private val db by lazy {
        Room.databaseBuilder(
            applicationContext, AlarmDatabase::class.java, "alarms.db"
        ).build()
    }
*/
    /*private val viewModel by viewModels<AlarmViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return AlarmViewModel(db.dao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemoteAlarmApp()
        }
    }
}