package assimp.blender

import assimp.*
import glm_.func.rad
import glm_.test.*
import glm_.mat4x4.*
import glm_.vec3.*
import io.kotlintest.*

// TODO also test linked meshes

private data class TransformDescription(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f,
                                        val rotation: Float = 0f, val rX: Float = 0f, val rY: Float = 0f, val rZ: Float = 0f,
                                        val sX: Float = 1f, val sY: Float = 1f, val sZ: Float = 1f)
private val cubeDescriptions = mapOf(
		"CubeX1" to TransformDescription(x = 1f),
		"CubeY1" to TransformDescription(y = 1f),
		"CubeZ1" to TransformDescription(z = 1f),
		"CubeXN1" to TransformDescription(x = -1f),
		"CubeYN1" to TransformDescription(y = -1f),
		"CubeZN1" to TransformDescription(z = -1f),
		"CubeRX45" to TransformDescription(rotation = 45f.rad, rX = 1f),
		"CubeRY45" to TransformDescription(rotation = 45f.rad, rY = 1f),
		"CubeRZ45" to TransformDescription(rotation = 45f.rad, rZ = 1f),
		"CubeSX2" to TransformDescription(sX = 2f),
		"CubeSY2" to TransformDescription(sY = 2f),
		"CubeSZ2" to TransformDescription(sZ = 2f)
                                     )

object testCubeRotateScaleTranslate {

	private fun generateTrans(des: TransformDescription): Mat4 {
		return generateTrans(des.x, des.y, des.z,
		              des.rotation, des.rX, des.rY, des.rZ,
		              des.sX, des.sY, des.sZ)
	}

	private fun AiNode.check(des: TransformDescription) {



		transformation shouldBe (generateTrans(des) plusOrMinus epsilon)

		numChildren shouldBe 0
		numMeshes shouldBe 1
	}

	operator fun invoke(fileName: String) {

		Importer().testFile(getResource(fileName)) {

			flags shouldBe 0

			with(rootNode) {

				name shouldBe "<BlenderRoot>"   // TODO should this be the filename instead??
				transformation shouldBe Mat4()
				numChildren shouldBe 12

				assertSoftly {
					cubeDescriptions.forEach { (name, description) ->
						children.first { it.name == name }.check(description)
					}
				}

				numMeshes shouldBe 0
				numLights shouldBe 0
				numCameras shouldBe 0
				numTextures shouldBe 0
				numAnimations shouldBe 0
			}

			numMeshes shouldBe 12
			repeat(12) { meshInd ->
				with(meshes[meshInd]) {
					primitiveTypes shouldBe AiPrimitiveType.POLYGON.i
					numVertices shouldBe 24
					numFaces shouldBe 6

					vertices[0] shouldBe Vec3(-0.5, -0.5, +0.5)
					vertices[5] shouldBe Vec3(+0.5, +0.5, +0.5)
					vertices[10] shouldBe Vec3(+0.5, -0.5, -0.5)
					vertices[15] shouldBe Vec3(+0.5, -0.5, -0.5)
					vertices[20] shouldBe Vec3(+0.5, -0.5, +0.5)
					vertices[23] shouldBe Vec3(-0.5, -0.5, +0.5)

					var i = 0
					faces.forEach {
						it.size shouldBe 4
						it shouldBe mutableListOf(i++, i++, i++, i++)
					}
				}
			}

			numMaterials shouldBe 1
			with(materials[0]) {
				name shouldBe AI_DEFAULT_MATERIAL_NAME
				// shadingModel shouldBe AiShadingMode.gouraud  TODO ???
				with(color!!) {
					ambient shouldBe Vec3()
					diffuse shouldBe Vec3(0.6)
					specular shouldBe Vec3(0.6)
					emissive shouldBe null
					shininess shouldBe null
					opacity shouldBe null
					refracti shouldBe null
				}
			}
		}
	}

}