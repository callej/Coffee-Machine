package machine

import kotlin.reflect.KFunction2

data class Ingredient(val name: String, var amount: Int, val unit: String) {
    override fun toString() = "$amount $unit of $name"
}

enum class Products(val ingredients: List<Ingredient>, val cost: Int) {
    ESPRESSO(listOf(
        Ingredient("water", 250, "ml"),
        Ingredient("coffee beans", 16, "g")),
        4
    ),
    LATTE(listOf(
        Ingredient("water", 350, "ml"),
        Ingredient("milk", 75, "ml"),
        Ingredient("coffee beans", 20, "g")),
        7
    ),
    CAPPUCCINO(listOf(
        Ingredient("water", 200, "ml"),
        Ingredient("milk", 100, "ml"),
        Ingredient("coffee beans", 12, "g")),
        6
    )
}

enum class Supplies(val item: String, val unit: String, val fillPhrase: String) {
    WATER("water", "ml", "Write how many ml of water do you want to add: "),
    MILK("milk", "ml", "Write how many ml of milk do you want to add: "),
    BEANS("coffee beans", "g", "Write how many grams of coffee beans do you want to add: "),
    CUPS("cups", "", "Write how many disposable cups of coffee do you want to add: ");

    companion object {
        fun phrase(item: String): String {
            for (supply in values()) {
                if (item == supply.item) return supply.fillPhrase
            }
            return ""
        }
        fun unit(item: String): String {
            for (supply in values()) {
                if (item == supply.item) return supply.unit
            }
            return ""
        }
    }
}

data class CoffeeMachine(val ingredients: MutableMap<String, Ingredient> = mutableMapOf(
    "water" to Ingredient("water", 400, "ml"),
    "milk" to Ingredient("milk", 540, "ml"),
    "coffee beans" to Ingredient("coffee beans", 120, "g")),
                         var cups: Int = 9, var money: Int = 550) {

    private lateinit var state: States

    init {
        for (supply in Supplies.values()) {
            States.FILL.info.add(supply.item)
        }
        States.FILL.subState = States.FILL.info.first()
        changeToState(States.MAIN_MENU)
    }

    fun input(command: String): Boolean {
        when (state) {
            States.MAIN_MENU -> States.MAIN_MENU.action(this, command)
            States.BUY -> States.BUY.action(this, command)
            States.FILL -> States.FILL.action(this, command)
            States.STOP -> States.STOP.action(this, command)
        }
        return state.machineOn
    }

    enum class States(val action: KFunction2<CoffeeMachine, String, Unit>, val info: MutableList<String>, var subState: String, val machineOn: Boolean) {
        MAIN_MENU(CoffeeMachine::mainActions, emptyList<String>().toMutableList(),"", true),
        BUY(CoffeeMachine::buyActions, emptyList<String>().toMutableList(),"", true),
        FILL(CoffeeMachine::fill, emptyList<String>().toMutableList(),"", true),
        STOP(CoffeeMachine::nop, emptyList<String>().toMutableList(),"", false)
    }

    private fun changeToState(newState: States) {
        when (newState) {
            States.MAIN_MENU -> print(mainMenu())
            States.BUY -> print(buyMenu())
            States.FILL -> print(Supplies.phrase(States.FILL.subState))
            States.STOP -> {}
        }
        state = newState
    }

    fun nop(noCommand: String) {}

    private fun mainMenu() = "\nWrite action (buy, fill, take, remaining, exit): "

    fun mainActions(input: String) {
        when (input) {
            "buy" -> changeToState(States.BUY)
            "fill" -> changeToState(States.FILL)
            "take" -> { money = 0; changeToState(States.MAIN_MENU) }
            "remaining" -> { println(this); changeToState(States.MAIN_MENU) }
            "exit" -> changeToState(States.STOP)
            else -> changeToState(States.MAIN_MENU)
        }
    }

    private fun buyMenu(): String {
        var options = "\nWhat do you want to buy? "
        for (item in Products.values()) {
            options += "${item.ordinal + 1} - ${item.name.lowercase()}, "
        }
        return options + "back - to main menu: "
    }

    fun buyActions(input: String) {
        if (input == "back") {
            changeToState(States.MAIN_MENU)
        } else if (Regex("\\d+").matches(input) && input.toInt() in 1..Products.values().size) {
            val choice = Products.values()[input.toInt() - 1]
            var enough = true
            for (ingredient in choice.ingredients) {
                if (ingredient.name !in ingredients.keys || ingredient.amount > ingredients[ingredient.name]!!.amount) {
                    println("Sorry, not enough ${ingredient.name}!")
                    enough = false
                } else if (enough && cups > 0){
                    ingredients[ingredient.name]!!.amount -= ingredient.amount
                }
            }
            if (cups == 0) {
                println("Sorry, not enough cups!")
                enough = false
            } else if (enough) {
                cups--
            }
            if (enough) {
                money += choice.cost
                println("I have enough resources, making you a coffee!")
            }
            changeToState(States.MAIN_MENU)
        }
    }

    fun fill(amount: String) {
        if (Regex("\\d+").matches(amount)) {
            when (state.subState) {
                in ingredients.keys -> ingredients[state.subState]!!.amount += amount.toInt()
                "cups" -> cups += amount.toInt()
                else -> ingredients[state.subState] = Ingredient(state.subState, amount.toInt(), Supplies.unit(state.subState))
            }
            if (state.subState == state.info.last()) {
                state.subState = state.info.first()
                changeToState(States.MAIN_MENU)
            } else {
                state.subState = state.info[state.info.indexOf(state.subState) + 1]
                changeToState(States.FILL)
            }
        }
    }

    override fun toString(): String {
        var content = "\nThe coffee machine has:\n"
        for (ingredient in ingredients.values) {
            content += ingredient.toString() + "\n"
        }
        content += "$cups disposable cups\n$$money of money"
        return content
    }
}