/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2018, assimp team


All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/

package assimp.format.blender

import assimp.NUL
import glm_.s

/** @file  BlenderScene.h
 *  @brief Intermediate representation of a BLEND scene.
 */

// Minor parts of this file are extracts from blender data structures,
// declared in the ./source/blender/makesdna directory.
// Stuff that is not used by Assimp is commented.


// NOTE
// this file serves as input data to the `./scripts/genblenddna.py`
// script. This script generates the actual binding code to read a
// blender file with a possibly different DNA into our structures.
// Only `struct` declarations are considered and the following
// rules must be obeyed in order for the script to work properly:
//
// * C++ style comments only
//
// * Structures may include the primitive types char, int, short,
//   float, double. Signed specifiers are not allowed on
//   integers. Enum types are allowed, but they must have been
//   defined in this header.
//
// * Structures may aggregate other structures, unless not defined
//   in this header.
//
// * Pointers to other structures or primitive types are allowed.
//   No references or double pointers or arrays of pointers.
//   A pointer to a T is normally written as std::shared_ptr, while a
//   pointer to an array of elements is written as boost::
//   shared_array. To avoid cyclic pointers, use raw pointers in
//   one direction.
//
// * Arrays can have maximally two-dimensions. Any non-pointer
//   type can form them.
//
// * Multiple fields can be declare in a single line (i.e `int a,b;`)
//   provided they are neither pointers nor arrays.
//
// * One of WARN, FAIL can be appended to the declaration (
//   prior to the semicolon to specify the error handling policy if
//   this field is missing in the input DNA). If none of those
//   is specified the default policy is to substitute a default
//   value for the field.
//


val AI_BLEND_MESH_MAX_VERTS = 2000000000L

var maxNameLen = 1024

class Id : ElemBase() {
    var name = ""
    var flag = 0.s
}

class ListBase : ElemBase() {
    var first: ElemBase? = null
    var last: ElemBase? = null
}

class PackedFile : ElemBase() {
    var size = 0
    var seek = 0
    var data: FileOffset? = null
}

class GroupObject : ElemBase() {
    var prev: GroupObject? = null
    var next: GroupObject? = null
    var ob: Object? = null
}

class Group : ElemBase() {
    var id = Id()
    var layer = 0

    var gObject: GroupObject? = null
}

class World : ElemBase() {
    var id = Id()
}

class MVert : ElemBase() {
    val co = FloatArray(3)
    val no = FloatArray(3)
    var flag = '\u0000'
    var matNr = 0
    var weight = 0
}

class MEdge : ElemBase() {
    var v1 = 0
    var v2 = 0
    var crease = '\u0000'
    var weight = '\u0000'
    var flag = 0.s
}

class MLoop : ElemBase() {
    var v = 0
    var e = 0
}

class MLoopUV : ElemBase() {
    val uv = FloatArray(2)
    var flag = 0
}

/** Note that red and blue are not swapped, as with MCol    */
class MLoopCol : ElemBase() {
    var r = '\u0000'
    var g = '\u0000'
    var b = '\u0000'
    var a = '\u0000'
}

class MPoly : ElemBase() {
    var loopStart = 0
    var totLoop = 0
    var matNr = 0.s
    var flag = '\u0000'
}

class MTexPoly : ElemBase() {
    var tpage: Image? = null
    var flag = '\u0000'
    var transp = '\u0000'
    var mode = 0.s
    var tile = 0.s
    var pad = 0.s
}

class MCol : ElemBase() {
    var r = '\u0000'
    var g = '\u0000'
    var b = '\u0000'
    var a = '\u0000'
}

class MFace : ElemBase() {
    var v1 = 0
    var v2 = 0
    var v3 = 0
    var v4 = 0
    var matNr = 0
    var flag = '\u0000'
}


class TFace : ElemBase() {
    var uv = Array(4) { FloatArray(2) }
    var col = IntArray(4)
    var flag = '\u0000'
    var mode = 0.s
    var tile = 0.s
    var unwrap = 0.s
}

class MTFace : ElemBase() {

    var uv = Array(4) { FloatArray(2) }
    var flag: Char = '\u0000'
    var mode = 0.s
    var tile = 0.s
    var unwrap = 0.s

    var tpage: Image? = null
}

class MDeformWeight : ElemBase()  {
    var defNr = 0
    var weight = 0f
}

class MDeformVert : ElemBase()  {
    val dw = ArrayList<MDeformWeight>()
    var totWeight = 0
}

val MA_RAYMIRROR = 0x40000
val MA_TRANSPARENCY = 0x10000
val MA_RAYTRANSP = 0x20000
val MA_ZTRANSP = 0x00040

class Material : ElemBase() {

    var id = Id()

    var r = 0f
    var g = 0f
    var b = 0f
    var specr = 0f
    var specg = 0f
    var specb = 0f
    var har = 0
    var ambr = 0f
    var ambg = 0f
    var ambb = 0f
    var mirr = 0f
    var mirg = 0f
    var mirb = 0f
    var emit = 0f
    var rayMirror = 0f
    var alpha = 0f
    var ref = 0f
    var translucency = 0f
    var mode = 0
    var roughness = 0f
    var darkness = 0f
    var refrac = 0f

    var amb = 0f
    var ang = 0f
    var spectra = 0f
    var spec = 0f
    var zoffs = 0f
    var add = 0f
    var fresnelMir = 0f
    var fresnelMirI = 0f
    var fresnelTra = 0f
    var fresnelTraI = 0f
    var filter = 0f
    var txLimit = 0f
    var txFalloff = 0f
    var glossMir = 0f
    var glossTra = 0f
    var adaptThreshMir = 0f
    var adaptThreshTra = 0f
    var anisoGlossMir = 0f
    var distMir = 0f
    var hasize = 0f
    var flaresize = 0f
    var subsize = 0f
    var flareboost = 0f
    var strandSta = 0f
    var strandEnd = 0f
    var strandEase = 0f
    var strandSurfnor = 0f
    var strandMin = 0f
    var strandWidthfade = 0f
    var sbias = 0f
    var lbias = 0f
    var shadAlpha = 0f
    var param = 0f
    var rms = 0f
    var rampfacCol = 0f
    var rampfacSpec = 0f
    var friction = 0f
    var fh = 0f
    var reflect = 0f
    var fhdist = 0f
    var xyfrict = 0f
    var sssRadius = 0f
    var sssCol = 0f
    var sssError = 0f
    var sssScale = 0f
    var sssIor = 0f
    var sssColfac = 0f
    var sssTexfac = 0f
    var sssFront = 0f
    var sssBack = 0f

    var material_type = 0
    var flag = 0
    var rayDepth = 0
    var rayDepthTra = 0
    var sampGlossMir = 0
    var sampGlossTra = 0
    var fadetoMir = 0
    var shadeFlag = 0
    var flarec = 0
    var starc = 0
    var linec = 0
    var ringc = 0
    var prLamp = 0
    var prTexture = 0
    var mlFlag = 0
    var texco = 0
    var mapto = 0
    var rampShow = 0
    var pad3 = 0
    var dynamode = 0
    var pad2 = 0
    var sssFlag = 0
    var sssPreset = 0
    var shadowonlyFlag = 0
    var index = 0
    var vcolAlpha = 0
    var pad4 = 0

    var seed1 = NUL
    var seed2 = NUL

    var group: Group? = null

    var diffShader = 0
    var specShader = 0

    var mTex = Array(18) { MTex() }
}

class Mesh : ElemBase() {

    var id = Id()

    var totface = 0
    var totedge = 0
    var totvert = 0
    var totloop = 0
    var totpoly = 0

    var subdiv = 0.s
    var subdivr = 0.s
    var subsurftype = 0.s
    var smoothresh = 0.s

    // TODO those lists represent C vector, should I maybe represent them as emptyList instead of null default?
    var mface: List<MFace>? = null
    var mtface: List<MTFace>? = null
    var tface: List<TFace>? = null
    var mvert: List<MVert>? = null
    var medge: List<MEdge>? = null
    var mloop: List<MLoop>? = null
    var mloopuv: List<MLoopUV>? = null
    var mloopcol: List<MLoopCol>? = null
    var mpoly: List<MPoly>? = null
    var mtpoly: List<MTexPoly>? = null
    var dvert: List<MDeformVert>? = null
    var mcol: List<MCol>? = null

    var mat: List<Material?>? = null

}
//// -------------------------------------------------------------------------------
//struct Library : ElemBase {
//    ID id FAIL;
//
//    char name [240] WARN;
//    char filename [240] FAIL;
//    std::shared_ptr<Library> parent WARN;
//};
//

class Camera : ElemBase() {
    enum class Type { PERSP, ORTHO, INVALID;

        companion object {
            infix fun of(i: Int) = values().getOrElse(i, { INVALID })
        }
    }

    var id = Id()

    var type = Type.PERSP
    var flag = Type.PERSP
    var lens = 0f
    var sensorX = 0f
    var clipSta = 0f
    var clipEnd = 0f
}

class Lamp : ElemBase() {

    enum class FalloffType { Constant, InvLinear, InvSquare, INVALID;
        //,FalloffType_Curve    = 0x3
        //,FalloffType_Sliders  = 0x4
        val i = ordinal

        companion object {
            infix fun of(i: Int) = values().getOrElse(i, { INVALID })
        }
    }

    enum class Type { Local, Sun, Spot, Hemi, Area, INVALID;
        //,Type_YFPhoton    = 0x5
        val i = ordinal

        companion object {
            infix fun of(i: Int) = values().getOrElse(i, { INVALID })
        }
    }

    var id = Id()
    //AnimData *adt;

    var type = Type.Local
    var flags = 0.s

    //int mode;

    var colorModel = 0.s
    var totex = 0.s
    var r = 0f
    var g = 0f
    var b = 0f
    var k = 0f
    //float shdwr, shdwg, shdwb;

    var energy = 0f
    var dist = 0f
    var spotSize = 0f
    var spotBlend = 0f
    //float haint;

    var att1 = 0f
    var att2 = 0f
    //struct CurveMapping *curfalloff;
    var falloffType = FalloffType.Constant

    //float clipsta, clipend, shadspotsize;
    //float bias, soft, compressthresh;
    //short bufsize, samp, buffers, filtertype;
    //char bufflag, buftype;

    //short ray_samp, ray_sampy, ray_sampz;
    //short ray_samp_type;
    var areaShape = 0.s
    var areaSize = 0f
    var areaSizeY = 0f
    var areaSizeZ = 0f
    //float adapt_thresh;
    //short ray_samp_method;

    //short texact, shadhalostep;

    //short sun_effect_type;
    //short skyblendtype;
    //float horizon_brightness;
    //float spread;
    var sunBrightness = 0f
    //float sun_size;
    //float backscattered_light;
    //float sun_intensity;
    //float atm_turbidity;
    //float atm_inscattering_factor;
    //float atm_extinction_factor;
    //float atm_distance_factor;
    //float skyblendfac;
    //float sky_exposure;
    //short sky_colorspace;

    // int YF_numphotons, YF_numsearch;
    // short YF_phdepth, YF_useqmc, YF_bufsize, YF_pad;
    // float YF_causticblur, YF_ltradius;

    // float YF_glowint, YF_glowofs;
    // short YF_glowtype, YF_pad2;

    //struct Ipo *ipo;
    //struct MTex *mtex[18];
    // short pr_texture;

    //struct PreviewImage *preview;
}

class ModifierData : ElemBase() {

    enum class Type { None, Subsurf, Lattice, Curve, Build, Mirror, Decimate, Wave, Armature, Hook, Softbody, Boolean,
        Array, EdgeSplit, Displace, UVProject, Smooth, Cast, MeshDeform, ParticleSystem, ParticleInstance, Explode,
        Cloth, Collision, Bevel, Shrinkwrap, Fluidsim, Mask, SimpleDeform, Multires, Surface, Smoke, ShapeKey;

        val i = ordinal
    }

    var next: ElemBase? = null
    var prev: ElemBase? = null

    var type = 0
    var mode = 0
    var name = ""
}

class SubsurfModifierData : ElemBase()  {

    enum class Type { CatmullClarke, Simple;

        val i = ordinal
    }

    enum class Flags(val i: Int) {
        // some omitted
        SubsurfUV (1 shl 3)
    }

    var modifier = ModifierData()
    var subdivType = 0.s
    var levels = 0.s
    var renderLevels = 0.s
    var flags = 0.s
}

//// -------------------------------------------------------------------------------
//struct MirrorModifierData : ElemBase {
//
//    enum Flags {
//        Flags_CLIPPING = 1 < <0,
//        Flags_MIRROR_U = 1 < <1,
//        Flags_MIRROR_V = 1 < <2,
//        Flags_AXIS_X = 1 < <3,
//        Flags_AXIS_Y = 1 < <4,
//        Flags_AXIS_Z = 1 < <5,
//        Flags_VGROUP = 1 < <6
//    };
//
//    ModifierData modifier FAIL;
//
//    short axis, flag;
//    float tolerance;
//    std::shared_ptr<Object> mirror_ob;
//};

class Object : ElemBase() {

    var id = Id()

    enum class Type(val i: Int) {
        EMPTY(0),
        MESH(1),
        CURVE(2),
        SURF(3),
        FONT(4),
        MBALL(5),

        LAMP(10),
        CAMERA(11),

        WAVE(21),
        LATTICE(22);

        companion object {
            infix fun of(i: Int) = values().first { it.i == i }
        }
    }

    var type = Type.EMPTY
    val obmat = Array(4) { FloatArray(4) }
    val parentinv = Array(4) { FloatArray(4) }
    var parSubstr = ""

    var parent: Object? = null
    var track: Object? = null

    var proxy: Object? = null
    var proxyFrom: Object? = null
    var proxyGroup: Object? = null
    var dupGroup: Group? = null
    var data: ElemBase? = null

    var modifiers = ListBase()
}


class Base : ElemBase() {
    var prev: Base? = null
    var next: Base? = null
    var object_: Object? = null
}

class Scene : ElemBase() {

    var id = Id()

    var camera: Object? = null
    var world: World? = null
    var basact: Base? = null

    var base = ListBase()
}

class Image : ElemBase() {
    var id = Id()

    var name = ""

    //struct anim *anim;

    var ok = 0
    var flag = 0
    var source = 0
    var type = 0
    var pad = 0
    var pad1 = 0
    var lastFrame = 0

    var tPageFlag = 0
    var totBind = 0
    var xRep = 0
    var yRep = 0
    var twsta = 0
    var twend = 0
    //unsigned int bindcode;
    //unsigned int *repbind;

    var packedfile: PackedFile? = null
    //struct PreviewImage * preview;

    var lastUpdate = 0f
    var lastUsed = 0
    var animSpeed = 0

    var genX = 0
    var genY = 0
    var genType = 0
}

class Tex : ElemBase() {

    /** actually, the only texture type we support is Type.Image    */
    enum class Type { Clouds, Wood, Marble, Magic, Blend, Stucci, Noise, Image, Plugin, EnvMap, Musgrave, Voronoi,
        DistortedNoise, PointDensitz, VoxelData;

        val i = ordinal
    }

    enum class ImageFlags(val i: Int) { INTERPOL(1), USEALPHA(2), MIPMAP(4), IMAROT(16), CALCALPHA(32),
        NORMALMAP(2048), GAUSS_MIP(4096), FILTER_MIN(8192), DERIVATIVEMAP(16384)
    }

    var id = Id()
    // AnimData *adt;

    //float noisesize, turbul;
    //float bright, contrast, rfac, gfac, bfac;
    //float filtersize;

    //float mg_H, mg_lacunarity, mg_octaves, mg_offset, mg_gain;
    //float dist_amount, ns_outscale;

    //float vn_w1;
    //float vn_w2;
    //float vn_w3;
    //float vn_w4;
    //float vn_mexp;
    //short vn_distm, vn_coltype;

    //short noisedepth, noisetype;
    //short noisebasis, noisebasis2;

    //short flag;
    var imaFlag = ImageFlags.INTERPOL
    var type = Type.Clouds
    //short stype;

    //float cropxmin, cropymin, cropxmax, cropymax;
    //int texfilter;
    //int afmax;
    //short xrepeat, yrepeat;
    //short extend;

    //short fie_ima;
    //int len;
    //int frames, offset, sfra;

    //float checkerdist, nabla;
    //float norfac;

    //ImageUser iuser;

    //bNodeTree *nodetree;
    //Ipo *ipo;
    var ima = Image()
    //PluginTex *plugin;
    //ColorBand *coba;
    //EnvMap *env;
    //PreviewImage * preview;
    //PointDensity *pd;
    //VoxelData *vd;

    //char use_nodes;
}

class MTex : ElemBase() {

    enum class Projection { N, X, Y, Z;

        val i = ordinal

        companion object {
            infix fun of(i: Int) = values()[i]
        }
    }

    enum class Flag(val i: Int) { RGBTOINT(0x1), STENCIL(0x2), NEGATIVE(0x4), ALPHAMIX(0x8), VIEWSPACE(0x10) }

    enum class BlendType { BLEND, MUL, ADD, SUB, DIV, DARK, DIFF, LIGHT, SCREEN, OVERLAY, BLEND_HUE, BLEND_SAT, BLEND_VAL, BLEND_COLOR;

        val i = ordinal

        companion object {
            infix fun of(i: Int) = values()[i]
        }
    }

    enum class MapType { COL, NORM, COLSPEC, COLMIR, REF, SPEC, EMIT, ALPHA, HAR, RAYMIRR, TRANSLU, AMB, DISPLACE, WARP;

        val i = 1 shl ordinal

        companion object {
            infix fun of(i: Int) = values()[i]
        }
    }

    // short texco, maptoneg;
    var mapTo = MapType.COL

    var blendType = BlendType.BLEND
    var object_: Object? = null
    var tex: Tex? = null
    var uvName = ""

    var projX = Projection.X
    var projY = Projection.Y
    var projZ = Projection.Z
    var mapping = ""
    val ofs = FloatArray(3)
    val size = FloatArray(3)
    var rot = 0f

    var texFlag = 0
    var colorModel = 0
    var pMapTo = 0
    var pMapToNeg = 0
    //short normapspace, which_output;
    //char brush_map_mode;
    var r = 0f
    var g = 0f
    var b = 0f
    var k = 0f
    //float def_var, rt;

    //float colfac, varfac;

    var norFac = 0f
    //float dispfac, warpfac;
    var colSpecFac = 0f
    var mirrFac = 0f
    var alphaFac = 0f
    var diffFac = 0f
    var specFac = 0f
    var emitFac = 0f
    var hardFac = 0f
    //float raymirrfac, translfac, ambfac;
    //float colemitfac, colreflfac, coltransfac;
    //float densfac, scatterfac, reflfac;

    //float timefac, lengthfac, clumpfac;
    //float kinkfac, roughfac, padensfac;
    //float lifefac, sizefac, ivelfac, pvelfac;
    //float shadowfac;
    //float zenupfac, zendownfac, blendfac;
}