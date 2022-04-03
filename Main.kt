package machine

fun main() {
    val coffeeMaker = CoffeeMachine()
    while (coffeeMaker.input(readln())) {}
}