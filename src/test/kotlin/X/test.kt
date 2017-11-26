package X

import assimp.Importer
import io.kotlintest.specs.StringSpec
import assimp.models

class test : StringSpec() {
    init {
        val test = "test.x"
        test {
            with(Importer().readFile(x + "test.x")!!) {
                printNodeNames(rootNode)
            }
        }
        val test_c = "test.assbin"
        test_c {
            val model2 = Importer().readFile(x_ass + "test.assbin")!!
            val model1 = Importer().readFile(x + "test.x")!!

            compareScenes(model1, model2)
        }

    }
}