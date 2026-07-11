package parfait.bootstrap

import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@AutoConfigurationPackage(basePackages = ["parfait"])
@SpringBootApplication(scanBasePackages = ["parfait"])
class ParfaitApplication

fun main(args: Array<String>) {
    runApplication<ParfaitApplication>(*args)
}
