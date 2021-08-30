package com.example.myapplication00

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.myapplication00.databinding.FragmentSecondScreenBinding
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.StringBuilder
import java.util.*
import kotlin.random.Random

const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2

class SecondScreenFragment : Fragment() {
    lateinit var binding: FragmentSecondScreenBinding

    //flag that indicates that animation is going
    var animationFlag: Boolean = false

    //variables for wheel animation
    var degrees: Int = 0

    //variables for wheel functionality
    val wheelValues: Array<Int> = arrayOf(
        1,
        300,
        400,
        600,
        0,
        900,
        3,
        500,
        900,
        300,
        400,
        550,
        800,
        500,
        300,
        500,
        600,
        2500,
        600,
        300,
        700,
        450,
        350,
        800
    )
    var wheelValue: Int = 1000
    var money: Int = 0
    var moneyCache: Int = 0

    //variable which indicates game phases like spinning wheel or guessing letters ect.
    var phaseNumber = 1

    //variables which contains the word that player is trying to guess and letters which he tried to guess but missed
    var word: String = ""
    var missedLetters = ""

    //variable which indicates rounds of game (there are 5 rounds total)
    var round = 1

    var chosenWords: List<String> = emptyList()

    //assigning file paths
    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val folder = File(path, "/KoloFortuny")
    val file = File(folder, "/words.txt")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSecondScreenBinding.inflate(layoutInflater)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).connectedThread = (activity as MainActivity).ConnectedThread((activity as MainActivity).mBluetoothSocket!!)
        (activity as MainActivity).connectedThread.start()

        if((activity as MainActivity).isHost){
            var com = readWord()
            (activity as MainActivity).connectedThread.write("$com".toByteArray())
            Toast.makeText(this.context,"$com",Toast.LENGTH_LONG).show()
        }
        else{
            var mNumber = (activity as MainActivity).receivedMessage.toInt()
            Toast.makeText(this.context,"$mNumber",Toast.LENGTH_LONG).show()
            var com = readWord(mNumber)
            Toast.makeText(this.context,"$com",Toast.LENGTH_LONG).show()
        }


        //giving popup screens for user
        val intent = Intent(this.context, PopUpWindow::class.java)
        intent.putExtra("popuptext", "Runda 1")
        intent.putExtra("darkstatusbar", false)
        startActivity(intent)

        Handler().postDelayed({intent.putExtra("popuptext", "Zakręć kołem!")
            intent.putExtra("darkstatusbar", false)
            startActivity(intent)}, 2500)


        //go back action while clicking button
        binding.goBackButton.setOnClickListener {
            val action = R.id.action_secondScreenFragment_to_firstScreenFragment
            Navigation.findNavController(binding.root).navigate(action)
        }

        if (round != 1) {
            popUp("Zakręć kołem fortuny!")
        }

        //wheel animation and functionality on clicking button
        binding.startWheelButton.setOnClickListener {
            if (phaseNumber == 1 || phaseNumber == 3) {
                if (!animationFlag) {
                    animationFlag = true
                    var rotationValue = Random.nextInt(1080, 1440)
                    val rotation = RotateAnimation(
                        degrees.toFloat(),
                        (degrees + rotationValue).toFloat(),
                        binding.fortuneWheel.pivotX,
                        binding.fortuneWheel.pivotY
                    )
                    rotation.duration = 4000
                    rotation.fillAfter = true
                    rotation.interpolator = DecelerateInterpolator(0.8f)
                    degrees += rotationValue
                    binding.fortuneWheel.startAnimation(rotation)
                    animationFlag = false
                }
                while (degrees > 360) {
                    degrees -= 360
                }

                wheelValue = wheelValues[rounding((degrees.toDouble() / 15))]

                Handler().postDelayed({
                    if (wheelValue != 0 && wheelValue != 1 && wheelValue != 3) {
                        phaseNumber = 2
                        moneyCache += wheelValue
                    } else if (wheelValue == 0) {
                        money = 0
                        moneyCache = 0
                        binding.moneyAccount.text = "Stan konta: ${money}$"
                        popUp("Niestety zbankrutowałeś! Tracisz wszystkie środki")
                    } else if (wheelValue == 1) {
                        moneyCache = 0
                        popUp("Tracisz turę!")
                    } else if (wheelValue == 3) {
                        moneyCache += 300
                        popUp("Wylosowałeś możliwość dodatkowego zakręcenia kołem!")
                    }
                }, 4000)
                if (wheelValue != 0 && wheelValue != 1 && wheelValue != 3)
                    Handler().postDelayed({ popUp("Zgadnij spółgłoskę!") }, 4000)

            } else if (phaseNumber == 2) {
                val toast = Toast.makeText(
                    this.context,
                    "Nie możesz teraz zakręcić kołem!",
                    Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }

        //guessing word functionality on clicking button
        binding.wordPushButton.setOnClickListener {
            if (phaseNumber == 3) {
                if (binding.guessWord.text.toString().uppercase() == word) {
                    if (round != 5) {
                        popUp("Brawo zgadłeś! Hasło to ${word} wygrywasz: ${money}$")
                        round++
                        missedLetters = ""
                        phaseNumber = 1
                        //readWord()
                        moneyCache = 0
                        resetLetterButtonsColor()
                        Handler().postDelayed({ popUp("RUNDA ${round}") }, 2500)
                        binding.roundNumberText.text = "Runda ${round}"
                    } else if (round == 5) {
                        popUp("Brawo odgadłeś wszystkie hasła! wygrywasz ${money}$")
                        Navigation.findNavController(view).popBackStack()
                    }
                } else {
                    popUp("Niestety nie udało ci się zgadnąć hasła!")
                }
                binding.guessWord.text.clear()
                phaseNumber = 1
            } else {
                val toast = Toast.makeText(
                    this.context,
                    "Nie możesz teraz zgadywać hasła!",
                    Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }

        letterButtons()
    }

    //function that draws word from file
    fun readWord(mNumber: Int = -1):Int {
        val bufferedReader = file.bufferedReader()
        val text: List<String> = bufferedReader.readLines()

        var number: Int = Random.nextInt(0, text.size - 1)
        while (chosenWords.contains(text[number])) {
            number = Random.nextInt(0, text.size - 1)
        }
        if (number != 0) {
            if (number % 2 != 0)
                number -= 1
        }

        if (mNumber != -1){
            number = mNumber
        }
        word = text[number]

        var underlines: String = ""

        for (i in 0..word.length - 1) {
            if (word[i] == ' ')
                underlines += "   "
            else
                underlines += " _ "
        }

        chosenWords.plus(word)
        binding.category.text = text[number + 1]
        binding.word.text = underlines
        return number
    }

    //function that changes wheel degree divided by 15 to table index
    fun rounding(number: Double): Int {
        var temp = number
        temp -= temp.toInt()
        if (temp > 0.49)
            if (number.toInt() + 1 == 24)
                return 0
            else
                return number.toInt() + 1
        else
            if (number.toInt() == 24)
                return 0
            else
                return number.toInt()
    }

    //function which checks guessing letters
    fun checkLetter(letter: Char): Boolean {
        var letterFlag = 0
        var totalText = binding.word.text
        for (i in 0..missedLetters.length - 1) {
            if (letter == missedLetters[i]) {
                popUp("Już próbowałeś zgadnąć tą literę! spróbuj inną!")
                return false
            }
        }

        for (i in 0..binding.word.text.length - 1) {
            if (letter == binding.word.text[i]) {
                popUp("Już zgadłeś tą literę! Spróbuj innej!")
                return false
            }
        }

        var temp = 1
        var j = 0
        for (i in 0..word.length - 1) {
            if (letter == word[i]) {
                letterFlag = 1
                while (j < i) {
                    temp += 3
                    j++
                }
                val text = StringBuilder(totalText).also { it.setCharAt(temp, letter) }
                totalText = text.toString()
            }
        }
        if (letterFlag == 1) {
            binding.word.text = totalText.toString()
            popUp("Brawo! Zgadłeś spółgłoskę, kwota: ${moneyCache}$ ląduje na twoim koncie!")
            money += moneyCache
            binding.moneyAccount.text = "Stan konta: ${money}$"
            moneyCache = 0
            phaseNumber = 3
            Handler().postDelayed(
                { popUp("Zakręć kołem ponownie lub spróbuj zgadnąć hasło!") },
                2500
            )
            return true
        }
        missedLetters += letter
        popUp("Niestety nie udało się zgadnąć spółgłoski. Kwota: ${moneyCache}$ przepada!")
        moneyCache = 0
        phaseNumber = 3
        Handler().postDelayed({ popUp("Zakręć kołem ponownie lub spróbuj zgadnąć hasło!") }, 2500)
        return false
    }

    //function which implements letter buttons actions
    fun letterButtons() {
        val toast = Toast.makeText(this.context, "Zakręć kołem!", Toast.LENGTH_SHORT)
        binding.LetterB.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('B')
                binding.LetterB.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterC.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('C')
                binding.LetterC.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterD.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('D')
                binding.LetterD.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterF.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('F')
                binding.LetterF.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterH.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('H')
                binding.LetterH.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterG.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('G')
                binding.LetterG.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterJ.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('J')
                binding.LetterJ.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterK.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('K')
                binding.LetterK.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterL.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('L')
                binding.LetterL.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterM.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('M')
                binding.LetterM.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterN.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('N')
                binding.LetterN.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterP.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('P')
                binding.LetterP.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterR.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('R')
                binding.LetterR.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterS.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('S')
                binding.LetterS.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterT.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('T')
                binding.LetterT.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterV.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('V')
                binding.LetterV.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterW.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('W')
                binding.LetterW.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterX.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('X')
                binding.LetterX.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
        binding.LetterZ.setOnClickListener {
            if (phaseNumber == 2) {
                checkLetter('Z')
                binding.LetterZ.isEnabled = false
            } else if (phaseNumber == 1) {
                toast.show()
            }
        }
    }

    //function that makes popup screens
    fun popUp(text: String) {
        val intent = Intent(this.context, PopUpWindow::class.java)
        intent.putExtra("popuptext", text)
        intent.putExtra("darkstatusbar", false)
        startActivity(intent)
    }

    fun resetLetterButtonsColor() {
        binding.LetterB.isEnabled = true
        binding.LetterC.isEnabled = true
        binding.LetterD.isEnabled = true
        binding.LetterF.isEnabled = true
        binding.LetterG.isEnabled = true
        binding.LetterH.isEnabled = true
        binding.LetterJ.isEnabled = true
        binding.LetterK.isEnabled = true
        binding.LetterL.isEnabled = true
        binding.LetterM.isEnabled = true
        binding.LetterN.isEnabled = true
        binding.LetterP.isEnabled = true
        binding.LetterR.isEnabled = true
        binding.LetterS.isEnabled = true
        binding.LetterT.isEnabled = true
        binding.LetterV.isEnabled = true
        binding.LetterW.isEnabled = true
        binding.LetterX.isEnabled = true
        binding.LetterZ.isEnabled = true
    }
}