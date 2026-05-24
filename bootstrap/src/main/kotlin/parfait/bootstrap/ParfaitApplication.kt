package parfait.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["parfait"])
class ParfaitApplication

fun main(args: Array<String>) {
    runApplication<ParfaitApplication>(*args)
}