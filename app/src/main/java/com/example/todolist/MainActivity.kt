package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.room.*
import kotlinx.coroutines.launch

@Entity(tableName = "tarefas")
data class Tarefa(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descricao: String
)

@Dao
interface TarefaDao {
    @Insert
    suspend fun inserirTarefa(tarefa: Tarefa)

    @Query("SELECT * FROM tarefas")
    suspend fun buscarTarefas(): List<Tarefa>
}

@Database(entities = [Tarefa::class], version = 1)
abstract class TarefaDatabase : RoomDatabase() {
    abstract fun tarefaDao(): TarefaDao

    companion object {
        @Volatile
        private var INSTANCE: TarefaDatabase? = null

        fun getDatabase(context: android.content.Context): TarefaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TarefaDatabase::class.java,
                    "tarefa_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TarefaViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val tarefaDao = TarefaDatabase.getDatabase(application).tarefaDao()
    var listaTarefas = mutableStateListOf<Tarefa>()
        private set

    fun carregarTarefas() {
        viewModelScope.launch {
            listaTarefas.clear()
            listaTarefas.addAll(tarefaDao.buscarTarefas())
        }
    }

    fun inserirTarefa(titulo: String, descricao: String) {
        val novaTarefa = Tarefa(titulo = titulo, descricao = descricao)
        viewModelScope.launch {
            tarefaDao.inserirTarefa(novaTarefa)
            carregarTarefas()
        }
    }
}

class MainActivity : ComponentActivity() {
    private val tarefaViewModel: TarefaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoListTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TarefaScreen(
                        viewModel = tarefaViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TarefaScreen(viewModel: TarefaViewModel, modifier: Modifier = Modifier) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            onClick = {
                viewModel.inserirTarefa(titulo, descricao)
                titulo = ""
                descricao = ""
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Adicionar Tarefa")
        }

        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(viewModel.listaTarefas.size) { index ->
                val tarefa = viewModel.listaTarefas[index]
                Text("Tarefa: ${tarefa.titulo}, Descrição: ${tarefa.descricao}")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.carregarTarefas()
    }
}

@Composable
fun ToDoListTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun TarefaScreenPreview() {
    ToDoListTheme {
        TarefaScreen(viewModel = TarefaViewModel(android.app.Application()))
    }
}
