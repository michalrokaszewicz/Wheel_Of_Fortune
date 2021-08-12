package com.example.myapplication00

import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.myapplication00.databinding.ActivityMainBinding
import com.example.myapplication00.databinding.FragmentFirstScreenBinding
import java.io.File

class FirstScreenFragment : Fragment() {
    lateinit var binding: FragmentFirstScreenBinding

    lateinit var path: File
    lateinit var folder: File
    lateinit var file: File

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFirstScreenBinding.inflate(layoutInflater)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val button = binding.button
        val addNewWord = binding.addNewWord
        val newWord = binding.newWord

        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        folder = File(path,"/KoloFortuny")
        file = File(folder, "/words.txt")

        createFile()

        button.setOnClickListener{
            val action = R.id.action_firstScreenFragment_to_secondScreenFragment
            Navigation.findNavController(binding.root).navigate(action)
        }

        newWord.setOnClickListener {
            var word: String = newWord.toString()
            file.appendText(word)
        }
    }

    fun createFile(){
        if(!folder.exists()) {
            folder.mkdir()

            val basicWords: Array<Pair<String, String>> = arrayOf(Pair("Mieszko I", "historia polski"), Pair("Husaria", "historia polski"),
                Pair("Robert Lewandowski", "piłka nożna"), Pair("Spalony", "piłka nożna"), Pair("Karta graficzna", "komputer"), Pair("Procesor", "komputer"),
                Pair("Minecraft", "gry komputerowe"), Pair("Sonic", "gry komputerowe"), Pair( "Monsun", "pogoda"), Pair( "Cyklon", "pogoda"))

            var i: Int = 0

            while ( i < 10 ) {
                file.appendText("${basicWords[i].first} \t ${basicWords[i].second} \n")
                i++
            }
        }
    }
}