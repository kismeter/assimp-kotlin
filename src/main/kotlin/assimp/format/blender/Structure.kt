package assimp.format.blender

import assimp.*
import glm_.*
import kotlin.math.min
import kotlin.reflect.*
import assimp.format.blender.ErrorPolicy as Ep

/** Represents a data structure in a BLEND file. A Structure defines n fields and their locations and encodings the input stream. Usually, every
 *  Structure instance pertains to one equally-named data structure in the
 *  BlenderScene.h header. This class defines various utilities to map a
 *  binary `blob` read from the file to such a structure instance with
 *  meaningful contents. */
class Structure (val db: FileDatabase) {

    // publicly accessible members
    var name = ""
    val fields = ArrayList<Field>()
    val indices = mutableMapOf<String, Long>()

    var size = 0L

    var cacheIdx = -1L

    override fun toString(): String {       // TODO temp debug
        return "[Structure]: $name"
    }

    /** Access a field of the structure by its canonical name. The pointer version returns NULL on failure while
     *  the reference version raises an import error. */
    operator fun get(ss: String): Field {
        val index = indices[ss] ?: throw Exception("BlendDNA: Did not find a field named `$ss` in structure `$name`")
        return fields[index.i]
    }

    /** Access a field of the structure by its index */
    operator fun get(i: Long) = fields.getOrElse(i.i) { throw Error("BlendDNA: There is no field with index `$i` in structure `$name`") }

    override fun equals(other: Any?) = other is Structure && name == other.name // name is meant to be an unique identifier

    fun convertInt() = when (name) {
        "int"    -> db.reader.int.i
        "short"  -> db.reader.short.i
        "char"   -> db.reader.get().c.i
        "float"  -> db.reader.float.i
        "double" -> db.reader.double.i
        else     -> throw Error("Unknown source for conversion to primitive data type: $name")
    }

    /** Try to read an instance of the structure from the stream and attempt to convert to `T`. This is done by an
     *  appropriate specialization. If none is available, a compiler complain is the result.
     *  @param dest Destination value to be written
     *  @param db File database, including input stream. */
    val convertChar
        get() = when (name) {
        // automatic rescaling from char to float and vice versa (seems useful for RGB colors)
            "float" -> (db.reader.float * 255f).c
            "double" -> (db.reader.double * 255f).c
            "int"    -> db.reader.int.c
            "short"  -> db.reader.short.c
            "char"   -> db.reader.get().c
            else     -> throw Error("Unknown source for conversion to primitive data type: $name")
        }

    val convertShort
        get() = when (name) {
        // automatic rescaling from short to float and vice versa (seems to be used by normals)
            "float" -> {
                var f = db.reader.float
                if (f > 1f) f = 1f
                (f * 32767f).s
            }
            "double" -> (db.reader.double * 32767.0).s
            "int"    -> db.reader.int.s
            "short"  -> db.reader.short
            "char"   -> db.reader.get().c.s
            else     -> throw Error("Unknown source for conversion to primitive data type: $name")
        }

    //        return when (T::class) {
//            Int::class -> convertDispatcher(db)
//
    val convertFloat
        get() = when (name) {
            // automatic rescaling from char to float and vice versa (seems useful for RGB colors)
            "char"   -> db.reader.get() / 255f
            // automatic rescaling from short to float and vice versa (used by normals)
            "short"  -> db.reader.short / 32767f
            "int"    -> db.reader.int.f
            "float"  -> db.reader.float
            "double" -> db.reader.double.f
            else     -> throw Error("Unknown source for conversion to primitive data type: $name")
        }

    fun convertPointer() = if (db.i64bit) db.reader.long else db.reader.int.L

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + fields.hashCode()
        result = 31 * result + indices.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + cacheIdx.hashCode()
        return result
    }

    /** field parsing for 1d arrays */
    fun readFieldString(errorPolicy: Ep, name: String): String {

        var dest = ""
        val old = db.reader.pos
        try {
            val f = get(name)
            val s = db.dna[f.type]

            // is the input actually an array?
            if (f.flags hasnt FieldFlag.Array)
                throw Error("Field `$name` of structure `${this.name}` ought to be a string")

            db.reader.pos += f.offset.i

            val builder = StringBuilder()

            // size conversions are always allowed, regardless of error_policy
            for (i in 0 until f.arraySizes[0]) {
                val c = s.convertChar
                if (c != NUL) builder += c
                else break
            }

//            for(; i < M; ++i) {
//                _defaultInitializer<ErrorPolicy_Igno>()(out[i])
//            }
            dest = builder.toString()

        } catch (e: Exception) {
            error(errorPolicy, dest, e.message)
        }

        // and recover the previous stream position
        db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead

        return dest
    }

    /** field parsing for 1d arrays */
    fun readFieldFloatArray(errorPolicy: Ep, out: FloatArray, name: String) {

        val old = db.reader.pos
        try {
            val f = get(name)
            val s = db.dna[f.type]

            // is the input actually an array?
            if (f.flags hasnt FieldFlag.Array)
                throw Error("Field `$name` of structure `${this.name}` ought to be an array of size ${out.size}")

            db.reader.pos += f.offset.i

            // size conversions are always allowed, regardless of error_policy
            for (i in 0 until min(f.arraySizes[0].i, out.size))
                out[i] = s.convertFloat

//            for (; i < M; ++i) {
//                _defaultInitializer<ErrorPolicy_Igno>()(out[i])
//            }
        } catch (e: Exception) {
            error(errorPolicy, out, e.message)
        }

        // and recover the previous stream position
        db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead
    }

    /** field parsing for 1d arrays */
    fun readFieldIntArray(errorPolicy: Ep, out: IntArray, name: String) {

        val old = db.reader.pos
        try {
            val f = get(name)
            val s = db.dna[f.type]

            // is the input actually an array?
            if (f.flags hasnt FieldFlag.Array)
                throw Error("Field `$name` of structure `${this.name}` ought to be an array of size ${out.size}")

            db.reader.pos += f.offset.i

            // size conversions are always allowed, regardless of error_policy
            for (i in 0 until min(f.arraySizes[0].i, out.size))
                out[i] = s.convertInt()

//            for (; i < M; ++i) {
//                _defaultInitializer<ErrorPolicy_Igno>()(out[i])
//            }
        } catch (e: Exception) {
            error(errorPolicy, out, e.message)
        }

        // and recover the previous stream position
        db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead
    }

    /** field parsing for 2d arrays */
    fun <T> readFieldArray2(errorPolicy: Ep, out: Array<T>, name: String) {

        val old = db.reader.pos
        try {
            val f = get(name)
            val s = db.dna[f.type]

            // is the input actually an array?
            if (f.flags hasnt FieldFlag.Array) throw Error("Field `$name` of structure `${this.name}` ought to be an array of size ${out.size}*N")

            db.reader.pos += f.offset.i

            // size conversions are always allowed, regardless of error_policy
            for (i in 0 until min(f.arraySizes[0].i, out.size)) {

                val n = out[i]
                if (n is FloatArray) for (j in n.indices) n[j] = s.convertFloat
            }
        } catch (e: Exception) {
            error(errorPolicy, out, e.message)
        }

        // and recover the previous stream position
        db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead
    }

    private inline fun <T>readFieldPtrPrivate(errorPolicy: Ep, out: KMutableProperty0<T?>, name: String, nonRecursive: Boolean, resolve: (Ep, KMutableProperty0<T?>, Long, Field, Boolean) -> Boolean): Boolean {

        val old = db.reader.pos
        val ptrval: Long
        val f: Field
        try {
            f = get(name)

            // sanity check, should never happen if the genblenddna script is right
            if (f.flags hasnt FieldFlag.Pointer) throw Error("Field `$name` of structure `${this.name}` ought to be a pointer")

            db.reader.pos += f.offset.i
            ptrval = convertPointer()
            /*  actually it is meaningless on which Structure the Convert is called because the `Pointer` argument
                triggers a special implementation.             */
        } catch (e: Exception) {
            error(errorPolicy, out, e.message)
            out.set(null)
            return false
        }

        // resolve the pointer and load the corresponding structure
        val res = resolve(errorPolicy, out, ptrval, f, nonRecursive)
        // and recover the previous stream position
        if (!nonRecursive) db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead

        return res
    }

    /** field parsing for pointer or dynamic array types (std::shared_ptr)
     *  The return value indicates whether the data was already cached. */
    fun <T> readFieldPtr(errorPolicy: Ep, out: KMutableProperty0<T?>, name: String, nonRecursive: Boolean = false): Boolean
            = readFieldPtrPrivate(errorPolicy, out, name, nonRecursive) { ep, o, ptrVal, field, nonRec ->
        resolvePtr(ep, o, ptrVal, field, nonRec)
    }

    fun <T> readFieldPtrList(errorPolicy: Ep, out: KMutableProperty0<List<T>?>, name: String, nonRecursive: Boolean = false): Boolean =
            readFieldPtrPrivate(errorPolicy, out, name, nonRecursive) { ep, o, ptrVal, field, _ ->
                resolvePointerList(ep, o, ptrVal, field)
            }

    /** field parsing for static arrays of pointer or dynamic array types (std::shared_ptr[])
     *  The return value indicates whether the data was already cached. */
//    fun <T>readFieldPtr(out )[N], const char* name,
//    const FileDatabase& db) const
//

	private inline fun <T: Any> readFieldPrivate(errorPolicy: Ep, out: T, name: String, read: (Structure, T) -> Unit): T {

	    val old = db.reader.pos
	    try {
		    val f = get(name)
		    // find the structure definition pertaining to this field
		    val s = db.dna[f.type]

		    read(s, out)
	    } catch(e: Exception) {
		    error(errorPolicy, out, e.message)
	    }

	    // and recover the previous stream position
	    db.reader.pos = old


	    if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead

	    return out
    }

	/**
	 * field parsing for `normal` values
     * The return value indicates whether the data was already cached.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: Any> readField(errorPolicy: Ep, out: KMutableProperty0<T>, name: String): KMutableProperty0<T> {

	    return readFieldPrivate(errorPolicy, out, name) { s, o ->
		    when (o()) {
			    is Float -> (out as KMutableProperty0<Float>).set(s.convertFloat)
			    is Short -> (out as KMutableProperty0<Short>).set(s.convertShort)
			    is Int   -> (out as KMutableProperty0<Int>).set(s.convertInt())
			    is Char  -> (out as KMutableProperty0<Char>).set(s.convertChar)
			    else     -> throw Error("Field type is not yet supported")
		    }
	    }
    }

	fun readField(errorPolicy: Ep, out: Id, name: String): Id {
		return readFieldPrivate(errorPolicy, out, name) { s, o -> s.convert(o) }
	}

	fun readField(errorPolicy: Ep, out: ListBase, name: String): ListBase {
		return readFieldPrivate(errorPolicy, out, name) { s, o -> s.convert(o) }
	}

	fun readField(errorPolicy: Ep, out: CustomData, name: String): CustomData {
		return readFieldPrivate(errorPolicy, out, name) { s, o ->
			TODO()
		}
	}

	fun readField(errorPolicy: Ep, out: ModifierData, name: String): ModifierData {
		return readFieldPrivate(errorPolicy, out, name) { s, o ->
			TODO()
		}
	}

    @Suppress("UNCHECKED_CAST")
    fun <T> resolvePtr(errorPolicy: Ep, out: KMutableProperty0<T?>, ptrVal: Long, f: Field, nonRecursive: Boolean = false) = when {
        f.type == "ElemBase" || isElem -> resolvePointer(errorPolicy, out as KMutableProperty0<ElemBase?>, ptrVal)
        else -> resolvePointer(errorPolicy, out, ptrVal, f, nonRecursive)
//        out is FileOffset -> resolvePointer(out, ptrVal, f, nonRecursive)
//        else -> throw Error()
    }


    fun <T> resolvePointer(errorPolicy: Ep, out: KMutableProperty0<T?>, ptrVal: Long, f: Field, nonRecursive: Boolean = false): Boolean {

        out.set(null) // ensure null pointers work
        if (ptrVal == 0L) return false

        val s = db.dna[f.type]
        // find the file block the pointer is pointing to
        val block = locateFileBlockForAddress(ptrVal)

        // also determine the target type from the block header and check if it matches the type which we expect.
        val ss = db.dna[block.dnaIndex.L]
        if (ss !== s)
            throw Error("Expected target to be of type `${s.name}` but seemingly it is a `${ss.name}` instead")

        // try to retrieve the object from the cache
        db.cache.get(s, out, ptrVal)
        if (out() != null) return true

        // seek to this location, but save the previous stream pointer.
        val pOld = db.reader.pos
        db.reader.pos = block.start + (ptrVal - block.address).i
        // FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
        // I really ought to improve StreamReader to work with 64 bit indices exclusively.

        // continue conversion after allocating the required storage

        // TODO does this work with primitives? The question is does it need to? I don't think we will ever see a pointer to a primitive
        val (constructor, converter) = db.dna.converters[f.type] ?: run {
            error(errorPolicy, out, "Failed to find a converter for the `${f.type}` structure")
            return false
        }

        val num = block.size / ss.size.i
        if (num > 1) {

            val list = MutableList<ElemBase?>(num) { constructor() }

            @Suppress("UNCHECKED_CAST")
            out.set(list as T)

            // cache the object before we convert it to avoid cyclic recursion.
            db.cache.set(s, out, ptrVal)

            // if the non_recursive flag is set, we don't do anything but leave the cursor at the correct position to resolve the object.
            if (!nonRecursive) {
                for (i in 0 until num) {

                    // workaround for https://youtrack.jetbrains.com/issue/KT-16303
                    tempElemBase = list[i]
                    s.converter(::tempElemBase)
                    list[i] = tempElemBase
                }

                db.reader.pos = pOld
            }
        } else {

            @Suppress("UNCHECKED_CAST")
            out.set(constructor() as T)

            // cache the object before we convert it to avoid cyclic recursion.
            db.cache.set(s, out, ptrVal)

            // if the non_recursive flag is set, we don't do anything but leave the cursor at the correct position to resolve the object.
            if (!nonRecursive) {

                @Suppress("UNCHECKED_CAST")
                s.converter(out as KMutableProperty0<ElemBase?>)

                db.reader.pos = pOld
            }
        }

        if (!ASSIMP.BLENDER_NO_STATS && out() != null)
            ++db.stats.pointersResolved

        return false
    }

    fun resolvePointer(out: FileOffset?, ptrVal: Long) {
        // Currently used exclusively by PackedFile::data to represent a simple offset into the mapped BLEND file.
        TODO()
//        out.reset();
//        if (!ptrval.val) {
//                    return false;
//                }
//
//                // find the file block the pointer is pointing to
//                const FileBlockHead* block = LocateFileBlockForAddress(ptrval,db);
//
//        out =  std::shared_ptr< FileOffset > (new FileOffset());
//        out->val = block->start+ static_cast<size_t>((ptrval.val - block->address.val) );
//        return false;
    }

    fun <T> resolvePointer(out: ArrayList<T>, ptrVal: Long, f: Field): Boolean {
        /*  This is a function overload, not a template specialization. According to the partial ordering rules, it
            should be selected by the compiler for array-of-pointer inputs, i.e. Object::mats.  */

        TODO()
//        out.reset();
//        if (!ptrval.val) {
//                    return false;
//                }
//
//                // find the file block the pointer is pointing to
//                const FileBlockHead* block = LocateFileBlockForAddress(ptrval,db);
//        const size_t num = block->size / (db.i64bit?8:4);
//
//        // keep the old stream position
//        const StreamReaderAny::pos pold = db.reader->GetCurrentPos();
//        db.reader->SetCurrentPos(block->start+ static_cast<size_t>((ptrval.val - block->address.val) ));
//
//        bool res = false;
//        // allocate raw storage for the array
//        out.resize(num);
//        for (size_t i = 0; i< num; ++i) {
//        Pointer val;
//        Convert(val,db);
//
//        // and resolve the pointees
//        res = ResolvePointer(out[i],val,db,f) && res;
//    }
//
//        db.reader->SetCurrentPos(pold);
//        return res;
    }

    fun resolvePointer(errorPolicy: Ep, out: KMutableProperty0<ElemBase?>, ptrVal: Long): Boolean {

        isElem = false
        /*  Special case when the data type needs to be determined at runtime.
            Less secure than in the `strongly-typed` case.         */

        out.set(null)
        if (ptrVal == 0L) return false

        // find the file block the pointer is pointing to
        val block = locateFileBlockForAddress(ptrVal)

        // determine the target type from the block header
        val s = db.dna[block.dnaIndex.L]

        // try to retrieve the object from the cache
        db.cache.get(s, out, ptrVal)
        if (out() != null) return true

        // seek to this location, but save the previous stream pointer.
        val pOld = db.reader.pos
        db.reader.pos = block.start + (ptrVal - block.address).i
        // FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
        // I really ought to improve StreamReader to work with 64 bit indices exclusively.

        // continue conversion after allocating the required storage
        val (constructor, converter) = db.dna.getBlobToStructureConverter(s) ?: run {

            /*  this might happen if DNA::RegisterConverters hasn't been called so far or
                if the target type is not contained in `our` DNA.             */
            out.set(null)
            error(errorPolicy, out, "Failed to find a converter for the `${s.name}` structure")
            return false
        }

        // allocate the object hull
        out.set(constructor())

        /*  cache the object immediately to prevent infinite recursion in a circular list with a single element
            (i.e. a self-referencing element).         */
        db.cache.set(s, out, ptrVal)

        // and do the actual conversion
        s.converter(out)
        db.reader.pos = pOld

        /*  store a pointer to the name string of the actual type in the object itself. This allows the conversion code
            to perform additional type checking.         */
        out()!!.dnaType = s.name

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.pointersResolved

        return false
    }

    fun <T> resolvePointerList(errorPolicy: Ep, out: KMutableProperty0<List<T>?>, ptrVal: Long, field: Field): Boolean {
        // This is a function overload, not a template specialization. According to
        // the partial ordering rules, it should be selected by the compiler
        // for array-of-pointer inputs, i.e. Object::mats.

        out.set(null)

        if(ptrVal == 0L) return false

        val block = locateFileBlockForAddress(ptrVal)

        val num = block.size / db.pointerSize

        // keep the old stream position
        val pOld = db.reader.pos
        db.reader.pos = block.start + (ptrVal - block.address).i
        // FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
        // I really ought to improve StreamReader to work with 64 bit indices exclusively.

        var res = true // FIXME: check back with https://github.com/assimp/assimp/issues/2160

        out.set(MutableList(num) {
            val ptr = convertPointer()
            res = resolvePtr(errorPolicy, ::tempAny, ptr, field) || res // FIXME: check back with https://github.com/assimp/assimp/issues/2160
            @Suppress("UNCHECKED_CAST")

            tempAny as T
        })

        db.reader.pos = pOld

        return res
    }

    fun locateFileBlockForAddress(ptrVal: Long): FileBlockHead {

        /*  the file blocks appear in list sorted by with ascending base addresses so we can run a binary search to locate
            the pointer quickly.

            NOTE: Blender seems to distinguish between side-by-side data (stored in the same data block) and far pointers,
            which are only used for structures starting with an ID.
            We don't need to make this distinction, our algorithm works regardless where the data is stored.    */
        val it = db.entries.firstOrNull { it.address >= ptrVal } ?:
        /*  This is crucial, pointers may not be invalid. This is either a corrupted file or an attempted attack.   */
        throw Error("Failure resolving pointer 0x${ptrVal.toHexString}, no file block falls into this address range")
        if (ptrVal >= it.address + it.size)
            throw Error("Failure resolving pointer 0x${ptrVal.toHexString}, nearest file block starting at " +
                    "0x${it.address.toHexString} ends at 0x${(it.address + it.size).toHexString}")
        return it
    }

    fun <T>KMutableProperty0<T?>.setIfNull(value: T): T = this() ?: value.also { set(value) }
//
//    private :
//
//    // ------------------------------------------------------------------------------
//    template <typename T> T* _allocate(std::shared_ptr<T>& out , size_t& s)
//    const {
//        out = std::shared_ptr<T>(new T ())
//        s = 1
//        return out.get()
//    }
//
//    template <typename T> T* _allocate(vector<T>& out , size_t& s)
//    const {
//        out.resize(s)
//        return s ? &out.front() : NULL
//    }
//
//    // --------------------------------------------------------
//    template <int error_policy>
//    struct _defaultInitializer
//    {
//
//        template < typename T, unsigned int N>
//        void operator ()(T(& out)[N], const char* = NULL) {
//        for (unsigned int i = 0; i < N; ++i) {
//        out[i] = T()
//    }
//    }
//
//        template < typename T, unsigned int N, unsigned int M>
//        void operator ()(T(& out)[N][M], const char* = NULL) {
//        for (unsigned int i = 0; i < N; ++i) {
//        for (unsigned int j = 0; j < M; ++j) {
//        out[i][j] = T()
//    }
//    }
//    }
//
//        template < typename T >
//        void operator ()(T& out, const char* = NULL) {
//        out = T()
//    }
//    }

    fun convertObject(dest: KMutableProperty0<Object?>) {

        val d = dest.setIfNull(Object())

        readField(Ep.Fail, d.id, "id")
        readField(Ep.Fail, ::tempInt, "type")
        d.type = Object.Type.of(tempInt)
        readFieldArray2(Ep.Warn, d.obmat, "obmat")
        readFieldArray2(Ep.Warn, d.parentinv, "parentinv")
        d.parSubstr = readFieldString(Ep.Warn, "parsubstr")
        readFieldPtr(Ep.Warn, d::parent, "*parent")
        readFieldPtr(Ep.Warn, d::track, "*track")
        readFieldPtr(Ep.Warn, d::proxy, "*proxy")
        readFieldPtr(Ep.Warn, d::proxyFrom, "*proxy_from")
        readFieldPtr(Ep.Warn, d::proxyGroup, "*proxy_group")
        readFieldPtr(Ep.Warn, d::dupGroup, "*dup_group")
        isElem = true
        readFieldPtr(Ep.Fail, d::data, "*data")
        readField(Ep.Igno, d.modifiers, "modifiers")

        db.reader.pos += size.i
    }

    fun convertGroup(dest: KMutableProperty0<Group?>) {

        val d = dest.setIfNull(Group())

        readField(Ep.Fail, d.id, "id")
        readField(Ep.Igno, d::layer, "layer")
        readFieldPtr(Ep.Igno, d::gObject, "*gobject")

        db.reader.pos += size.i
    }

    fun convertMTex(dest: KMutableProperty0<MTex?>) {

        val d = dest.setIfNull(MTex())

        readField(Ep.Igno, ::tempInt, "mapto")
        d.mapTo = MTex.MapType of tempInt
        readField(Ep.Igno, ::tempInt, "blendtype")
        d.blendType = MTex.BlendType of tempInt
        readFieldPtr(Ep.Igno, d::object_, "*object")
        readFieldPtr(Ep.Igno, d::tex, "*tex")
        d.uvName = readFieldString(Ep.Igno, "uvname")
        readField(Ep.Igno, ::tempInt, "projx")
        d.projX = MTex.Projection of tempInt
        readField(Ep.Igno, ::tempInt, "projy")
        d.projY = MTex.Projection of tempInt
        readField(Ep.Igno, ::tempInt, "projz")
        d.projZ = MTex.Projection of tempInt
        d.mapping = readFieldString(Ep.Igno, "mapping")
        readFieldFloatArray(Ep.Igno, d.ofs, "ofs")
        readFieldFloatArray(Ep.Igno, d.size, "size")
        readField(Ep.Igno, d::rot, "rot")
        readField(Ep.Igno, d::texFlag, "texflag")
        readField(Ep.Igno, d::colorModel, "colormodel")
        readField(Ep.Igno, d::pMapTo, "pmapto")
        readField(Ep.Igno, d::pMapToNeg, "pmaptoneg")
        readField(Ep.Warn, d::r, "r")
        readField(Ep.Warn, d::g, "g")
        readField(Ep.Warn, d::b, "b")
        readField(Ep.Warn, d::k, "k")
        readField(Ep.Igno, d::colSpecFac, "colspecfac")
        readField(Ep.Igno, d::mirrFac, "mirrfac")
        readField(Ep.Igno, d::alphaFac, "alphafac")
        readField(Ep.Igno, d::diffFac, "difffac")
        readField(Ep.Igno, d::specFac, "specfac")
        readField(Ep.Igno, d::emitFac, "emitfac")
        readField(Ep.Igno, d::hardFac, "hardfac")
        readField(Ep.Igno, d::norFac, "norfac")

        db.reader.pos += size.i
    }

    fun convertTFace(dest: KMutableProperty0<TFace?>) {

        val d = dest.setIfNull(TFace())

        readFieldArray2(Ep.Fail, d.uv, "uv")
        readFieldIntArray(Ep.Fail, d.col, "col")
        readField(Ep.Igno, d::flag, "flag")
        readField(Ep.Igno, d::mode, "mode")
        readField(Ep.Igno, d::tile, "tile")
        readField(Ep.Igno, d::unwrap, "unwrap")

        db.reader.pos += size.i
    }

    fun convertSubsurfModifierData(dest: KMutableProperty0<SubsurfModifierData?>) {

        val d = dest.setIfNull(SubsurfModifierData())

        readField(Ep.Fail, d.modifier, "modifier")
        readField(Ep.Warn, d::subdivType, "subdivType")
        readField(Ep.Fail, d::levels, "levels")
        readField(Ep.Igno, d::renderLevels, "renderLevels")
        readField(Ep.Igno, d::flags, "flags")

        db.reader.pos += size.i
    }

    fun convertMFace(dest: KMutableProperty0<MFace?>) {

        val d = dest.setIfNull(MFace())

        readField(Ep.Fail, d::v1, "v1")
        readField(Ep.Fail, d::v2, "v2")
        readField(Ep.Fail, d::v3, "v3")
        readField(Ep.Fail, d::v4, "v4")
        readField(Ep.Fail, d::matNr, "mat_nr")
        readField(Ep.Igno, d::flag, "flag")

        db.reader.pos += size.i
    }

    fun convertLamp(dest: KMutableProperty0<Lamp?>) {

        val d = dest.setIfNull(Lamp())

        readField(Ep.Fail, d.id,"id")
        readField(Ep.Fail, ::tempInt, "type")
        d.type = Lamp.Type of tempInt
        readField(Ep.Igno, d::flags,"flags")
        readField(Ep.Igno, d::colorModel,"colormodel")
        readField(Ep.Igno, d::totex,"totex")
        readField(Ep.Warn, d::r,"r")
        readField(Ep.Warn, d::g,"g")
        readField(Ep.Warn, d::b,"b")
        readField(Ep.Warn, d::k,"k")
        readField(Ep.Igno, d::energy,"energy")
        readField(Ep.Igno, d::dist,"dist")
        readField(Ep.Igno, d::spotSize,"spotsize")
        readField(Ep.Igno, d::spotBlend,"spotblend")
        readField(Ep.Igno, d::att1,"att1")
        readField(Ep.Igno, d::att2,"att2")
        readField(Ep.Igno, ::tempInt, "falloff_type")
        d.falloffType = Lamp.FalloffType of tempInt
        readField(Ep.Igno, d::sunBrightness,"sun_brightness")
        readField(Ep.Igno, d::areaSize,"area_size")
        readField(Ep.Igno, d::areaSizeY,"area_sizey")
        readField(Ep.Igno, d::areaSizeZ,"area_sizez")
        readField(Ep.Igno, d::areaShape,"area_shape")

        db.reader.pos += size.i
    }

    fun convertMDeformWeight(dest: KMutableProperty0<MDeformWeight?>) {

        val d = dest.setIfNull(MDeformWeight())

        readField(Ep.Fail, d::defNr,"def_nr")
        readField(Ep.Fail, d::weight,"weight")

        db.reader.pos += size.i
    }

    fun convertPackedFile(dest: KMutableProperty0<PackedFile?>) {

        val d = dest.setIfNull(PackedFile())

        readField(Ep.Warn, d::size,"size")
        readField(Ep.Warn, d::seek,"seek")
        readFieldPtr(Ep.Warn, d::data,"*data")

        db.reader.pos += size.i
    }


    fun convertBase(dest: KMutableProperty0<Base?>) {
        /*  note: as per https://github.com/assimp/assimp/issues/128, reading the Object linked list recursively is
            prone to stack overflow.
            This structure converter is therefore an hand-written exception that does it iteratively.   */

        val initialPos = db.reader.pos

        var todo = dest.setIfNull(Base()) to initialPos
        while (true) {

            val curDest = todo.first
            db.reader.pos = todo.second

            /*  we know that this is a double-linked, circular list which we never traverse backwards,
                so don't bother resolving the back links.             */
            curDest.prev = null

            readFieldPtr(Ep.Warn, curDest::object_, "*object")

            /*  the return value of ReadFieldPtr indicates whether the object was already cached.
                In this case, we don't need to resolve it again.    */
            if (!readFieldPtr(Ep.Warn, curDest::next, "*next", true) && curDest.next != null) {
                todo = (curDest.next ?: Base().also { curDest.next = it }) to db.reader.pos
                continue
            }
	        break
        }

	    db.reader.pos = initialPos + size.i
    }

	fun convertMTFace(dest: KMutableProperty0<MTFace?>) {

		val d = dest.setIfNull(MTFace())

		readFieldArray2(Ep.Fail, d.uv, "uv")
		readField(Ep.Igno, d::flag, "flag")
		readField(Ep.Igno, d::tile,"tile")
        readField(Ep.Igno, d::unwrap,"unwrap")
	}

    fun convertMaterial(dest: KMutableProperty0<Material?>) {

        val d = dest.setIfNull(Material())

        readField(Ep.Fail, d.id,"id")
        readField(Ep.Warn, d::r,"r")
        readField(Ep.Warn, d::g,"g")
        readField(Ep.Warn, d::b,"b")
        readField(Ep.Warn, d::specr,"specr")
        readField(Ep.Warn, d::specg,"specg")
        readField(Ep.Warn, d::specb,"specb")
        readField(Ep.Igno, d::har,"har")
        readField(Ep.Warn, d::ambr,"ambr")
        readField(Ep.Warn, d::ambg,"ambg")
        readField(Ep.Warn, d::ambb,"ambb")
        readField(Ep.Igno, d::mirr,"mirr")
        readField(Ep.Igno, d::mirg,"mirg")
        readField(Ep.Igno, d::mirb,"mirb")
        readField(Ep.Warn, d::emit,"emit")
        readField(Ep.Igno, d::rayMirror,"ray_mirror")
        readField(Ep.Warn, d::alpha,"alpha")
        readField(Ep.Igno, d::ref,"ref")
        readField(Ep.Igno, d::translucency,"translucency")
        readField(Ep.Igno, d::mode,"mode")
        readField(Ep.Igno, d::roughness,"roughness")
        readField(Ep.Igno, d::darkness,"darkness")
        readField(Ep.Igno, d::refrac,"refrac")
        readFieldPtr(Ep.Igno, d::group,"*group")
        readField(Ep.Warn, d::diffShader,"diff_shader")
        readField(Ep.Warn, d::specShader,"spec_shader")
        readFieldPtr(Ep.Igno, d::mTex,"*mtex")

        readField(Ep.Igno, d::amb, "amb")
        readField(Ep.Igno, d::ang, "ang")
        readField(Ep.Igno, d::spectra, "spectra")
        readField(Ep.Igno, d::spec, "spec")
        readField(Ep.Igno, d::zoffs, "zoffs")
        readField(Ep.Igno, d::add, "add")
        readField(Ep.Igno, d::fresnelMir, "fresnel_mir")
        readField(Ep.Igno, d::fresnelMirI, "fresnel_mir_i")
        readField(Ep.Igno, d::fresnelTra, "fresnel_tra")
        readField(Ep.Igno, d::fresnelTraI, "fresnel_tra_i")
        readField(Ep.Igno, d::filter, "filter")
        readField(Ep.Igno, d::txLimit, "tx_limit")
        readField(Ep.Igno, d::txFalloff, "tx_falloff")
        readField(Ep.Igno, d::glossMir, "gloss_mir")
        readField(Ep.Igno, d::glossTra, "gloss_tra")
        readField(Ep.Igno, d::adaptThreshMir, "adapt_thresh_mir")
        readField(Ep.Igno, d::adaptThreshTra, "adapt_thresh_tra")
        readField(Ep.Igno, d::anisoGlossMir, "aniso_gloss_mir")
        readField(Ep.Igno, d::distMir, "dist_mir")
        readField(Ep.Igno, d::hasize, "hasize")
        readField(Ep.Igno, d::flaresize, "flaresize")
        readField(Ep.Igno, d::subsize, "subsize")
        readField(Ep.Igno, d::flareboost, "flareboost")
        readField(Ep.Igno, d::strandSta, "strand_sta")
        readField(Ep.Igno, d::strandEnd, "strand_end")
        readField(Ep.Igno, d::strandEase, "strand_ease")
        readField(Ep.Igno, d::strandSurfnor, "strand_surfnor")
        readField(Ep.Igno, d::strandMin, "strand_min")
        readField(Ep.Igno, d::strandWidthfade, "strand_widthfade")
        readField(Ep.Igno, d::sbias, "sbias")
        readField(Ep.Igno, d::lbias, "lbias")
        readField(Ep.Igno, d::shadAlpha, "shad_alpha")
        readField(Ep.Igno, d::param, "param")
        readField(Ep.Igno, d::rms, "rms")
        readField(Ep.Igno, d::rampfacCol, "rampfac_col")
        readField(Ep.Igno, d::rampfacSpec, "rampfac_spec")
        readField(Ep.Igno, d::friction, "friction")
        readField(Ep.Igno, d::fh, "fh")
        readField(Ep.Igno, d::reflect, "reflect")
        readField(Ep.Igno, d::fhdist, "fhdist")
        readField(Ep.Igno, d::xyfrict, "xyfrict")
        readField(Ep.Igno, d::sssRadius, "sss_radius")
        readField(Ep.Igno, d::sssCol, "sss_col")
        readField(Ep.Igno, d::sssError, "sss_error")
        readField(Ep.Igno, d::sssScale, "sss_scale")
        readField(Ep.Igno, d::sssIor, "sss_ior")
        readField(Ep.Igno, d::sssColfac, "sss_colfac")
        readField(Ep.Igno, d::sssTexfac, "sss_texfac")
        readField(Ep.Igno, d::sssFront, "sss_front")
        readField(Ep.Igno, d::sssBack, "sss_back")

        readField(Ep.Igno, d::material_type, "material_type")
        readField(Ep.Igno, d::flag, "flag")
        readField(Ep.Igno, d::rayDepth, "ray_depth")
        readField(Ep.Igno, d::rayDepthTra, "ray_depth_tra")
        readField(Ep.Igno, d::sampGlossMir, "samp_gloss_mir")
        readField(Ep.Igno, d::sampGlossTra, "samp_gloss_tra")
        readField(Ep.Igno, d::fadetoMir, "fadeto_mir")
        readField(Ep.Igno, d::shadeFlag, "shade_flag")
        readField(Ep.Igno, d::flarec, "flarec")
        readField(Ep.Igno, d::starc, "starc")
        readField(Ep.Igno, d::linec, "linec")
        readField(Ep.Igno, d::ringc, "ringc")
        readField(Ep.Igno, d::prLamp, "pr_lamp")
        readField(Ep.Igno, d::prTexture, "pr_texture")
        readField(Ep.Igno, d::mlFlag, "ml_flag")
        readField(Ep.Igno, d::diffShader, "diff_shader")
        readField(Ep.Igno, d::specShader, "spec_shader")
        readField(Ep.Igno, d::texco, "texco")
        readField(Ep.Igno, d::mapto, "mapto")
        readField(Ep.Igno, d::rampShow, "ramp_show")
        readField(Ep.Igno, d::pad3, "pad3")
        readField(Ep.Igno, d::dynamode, "dynamode")
        readField(Ep.Igno, d::pad2, "pad2")
        readField(Ep.Igno, d::sssFlag, "sss_flag")
        readField(Ep.Igno, d::sssPreset, "sss_preset")
        readField(Ep.Igno, d::shadowonlyFlag, "shadowonly_flag")
        readField(Ep.Igno, d::index, "index")
        readField(Ep.Igno, d::vcolAlpha, "vcol_alpha")
        readField(Ep.Igno, d::pad4, "pad4")

        readField(Ep.Igno, d::seed1, "seed1")
        readField(Ep.Igno, d::seed2, "seed2")
    }
////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<MTexPoly> (
//    MTexPoly& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        {
//            std::shared_ptr<Image> tpage;
//            ReadFieldPtr<ErrorPolicy_Igno>(tpage,"*tpage",db);
//            dest.tpage = tpage.get();
//        }
//        ReadField<ErrorPolicy_Igno>(dest.flag,"flag",db);
//        ReadField<ErrorPolicy_Igno>(dest.transp,"transp",db);
//        ReadField<ErrorPolicy_Igno>(dest.mode,"mode",db);
//        ReadField<ErrorPolicy_Igno>(dest.tile,"tile",db);
//        ReadField<ErrorPolicy_Igno>(dest.pad,"pad",db);
//
//        db.reader->IncPtr(size);
//    }
//

    fun convertMesh(dest: KMutableProperty0<Mesh?>) {

        val d = dest.setIfNull(Mesh())

	    readField(Ep.Fail, d.id, "id")
		readField(Ep.Fail, d::totface, "totface")
		readField(Ep.Fail, d::totedge, "totedge")
		readField(Ep.Fail, d::totvert, "totvert")
		readField(Ep.Igno, d::totloop, "totloop")
		readField(Ep.Igno, d::totpoly, "totpoly")
		readField(Ep.Igno, d::subdiv, "subdiv")
		readField(Ep.Igno, d::subdivr, "subdivr")
		readField(Ep.Igno, d::subsurftype, "subsurftype")
		readField(Ep.Igno, d::subsurftype, "subsurftype")
		readField(Ep.Igno, d::smoothresh, "smoothresh")
	    readFieldPtr(Ep.Fail, d::mface, "*mface")
	    readFieldPtr(Ep.Igno, d::mtface, "*mtface")
	    readFieldPtr(Ep.Igno, d::tface, "*tface")
	    readFieldPtr(Ep.Fail, d::mvert, "*mvert")
	    readFieldPtr(Ep.Warn, d::medge, "*medge")
	    readFieldPtr(Ep.Igno, d::mloop, "*mloop")
	    readFieldPtr(Ep.Igno, d::mloopuv, "*mloopuv")
	    readFieldPtr(Ep.Igno, d::mloopcol, "*mloopcol")
	    readFieldPtr(Ep.Igno, d::mpoly, "*mpoly")
	    readFieldPtr(Ep.Igno, d::mtpoly, "*mtpoly")
	    readFieldPtr(Ep.Igno, d::dvert, "*dvert")
	    readFieldPtr(Ep.Igno, d::mcol, "*mcol")
        readFieldPtrList(Ep.Fail, d::mat, "**mat")

	    readField(Ep.Igno, d.vdata, "vdata")
	    readField(Ep.Igno, d.edata, "edata")
	    readField(Ep.Igno, d.fdata, "fdata")
	    readField(Ep.Igno, d.pdata, "pdata")
	    readField(Ep.Igno, d.ldata, "ldata")

	    db.reader.pos += size.i
    }
////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<MDeformVert> (
//    MDeformVert& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadFieldPtr<ErrorPolicy_Warn>(dest.dw,"*dw",db);
//        ReadField<ErrorPolicy_Igno>(dest.totweight,"totweight",db);
//
//        db.reader->IncPtr(size);
//    }
//

    fun convertWorld(dest: KMutableProperty0<World?>) {

        val d = dest.setIfNull(World())

        readField(Ep.Fail, d.id, "id")

        db.reader.pos += size.i
    }

////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<MLoopCol> (
//    MLoopCol& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadField<ErrorPolicy_Igno>(dest.r,"r",db);
//        ReadField<ErrorPolicy_Igno>(dest.g,"g",db);
//        ReadField<ErrorPolicy_Igno>(dest.b,"b",db);
//        ReadField<ErrorPolicy_Igno>(dest.a,"a",db);
//
//        db.reader->IncPtr(size);
//    }
//

    fun convertMVert(dest: KMutableProperty0<MVert?>) {
        val d = dest.setIfNull(MVert())

        readFieldFloatArray(Ep.Fail, d.co, "co")
        readFieldFloatArray(Ep.Fail, d.no, "no")
        readField(Ep.Igno, d::flag, "flag")
        //readField(Ep.Warn, d.matNr, "matNr")
        readField(Ep.Igno, d::weight, "bweight")

    }

    fun convertMEdge(dest: KMutableProperty0<MEdge?>) {

        val d = dest.setIfNull(MEdge())

        readField(Ep.Fail, d::v1, "v1")
        readField(Ep.Fail, d::v2,"v2")
        readField(Ep.Igno, d::crease,"crease")
        readField(Ep.Igno, d::weight,"bweight")
        readField(Ep.Igno, d::flag,"flag")
    }
//
////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<MLoopUV> (
//    MLoopUV& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadFieldArray<ErrorPolicy_Igno>(dest.uv,"uv",db);
//        ReadField<ErrorPolicy_Igno>(dest.flag,"flag",db);
//
//        db.reader->IncPtr(size);
//    }
//

    fun convertGroupObject(dest: KMutableProperty0<GroupObject?>) {

        val d = dest.setIfNull(GroupObject())

        readFieldPtr(Ep.Fail, d::prev, "*prev")
        readFieldPtr(Ep.Fail, d::next, "*next")
        readFieldPtr(Ep.Igno, d::ob, "*ob")

        db.reader.pos += size.i
    }


    fun convert(dest: ListBase) {

        isElem = true
        readFieldPtr(Ep.Igno, dest::first, "*first")
        isElem = true
        readFieldPtr(Ep.Igno, dest::last, "*last")

        db.reader.pos += size.i
    }

////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<MLoop> (
//    MLoop& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadField<ErrorPolicy_Igno>(dest.v,"v",db);
//        ReadField<ErrorPolicy_Igno>(dest.e,"e",db);
//
//        db.reader->IncPtr(size);
//    }
//
////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<ModifierData> (
//    ModifierData& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadFieldPtr<ErrorPolicy_Warn>(dest.next,"*next",db);
//        ReadFieldPtr<ErrorPolicy_Warn>(dest.prev,"*prev",db);
//        ReadField<ErrorPolicy_Igno>(dest.type,"type",db);
//        ReadField<ErrorPolicy_Igno>(dest.mode,"mode",db);
//        ReadFieldArray<ErrorPolicy_Igno>(dest.name,"name",db);
//
//        db.reader->IncPtr(size);
//    }
//

	fun convertModifierData(data: ModifierData) {

		readFieldPtr(Ep.Warn, data::next,"*next")
        readFieldPtr(Ep.Warn, data::prev,"*prev")
        readField(Ep.Igno, data::type,"type")
        readField(Ep.Igno, data::mode,"mode")
        data.name = readFieldString(Ep.Igno, "name")
	}

    fun convertModifierDataRef(dest: KMutableProperty0<ModifierData?>) {

        val d = dest.setIfNull(ModifierData())
        convertModifierData(d)
    }

    fun convert(id: Id) {

        id.name = readFieldString(Ep.Warn, "name")
        readField(Ep.Igno, id::flag, "flag")

        db.reader.pos += size.i
    }

    fun convertMCol(dest: KMutableProperty0<MCol?>) {

        val d = dest.setIfNull(MCol())

        readField(Ep.Fail, d::r,"r")
        readField(Ep.Fail, d::g,"g")
        readField(Ep.Fail, d::b,"b")
        readField(Ep.Fail, d::a,"a")
    }

////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<MPoly> (
//    MPoly& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadField<ErrorPolicy_Igno>(dest.loopstart,"loopstart",db);
//        ReadField<ErrorPolicy_Igno>(dest.totloop,"totloop",db);
//        ReadField<ErrorPolicy_Igno>(dest.mat_nr,"mat_nr",db);
//        ReadField<ErrorPolicy_Igno>(dest.flag,"flag",db);
//
//        db.reader->IncPtr(size);
//    }
//

    fun convertScene(): Scene {

        val dest = Scene()

        readField(Ep.Fail, dest.id, "id")
        readFieldPtr(Ep.Warn, dest::camera, "*camera")
        readFieldPtr(Ep.Warn, dest::world, "*world")
        readFieldPtr(Ep.Warn, dest::basact, "*basact")
        readField(Ep.Igno, dest.base, "base")

        db.reader.pos += size.i

        return dest
    }

    //
////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<Library> (
//    Library& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadField<ErrorPolicy_Fail>(dest.id,"id",db);
//        ReadFieldArray<ErrorPolicy_Warn>(dest.name,"name",db);
//        ReadFieldArray<ErrorPolicy_Fail>(dest.filename,"filename",db);
//        ReadFieldPtr<ErrorPolicy_Warn>(dest.parent,"*parent",db);
//
//        db.reader->IncPtr(size);
//    }
//
////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<Tex> (
//    Tex& dest,
//    const FileDatabase& db
//    ) const
//    {
//        short temp_short = 0;
//        ReadField<ErrorPolicy_Igno>(temp_short,"imaflag",db);
//        dest.imaflag = static_cast<Assimp::Blender::Tex::ImageFlags>(temp_short);
//        int temp = 0;
//        ReadField<ErrorPolicy_Fail>(temp,"type",db);
//        dest.type = static_cast<Assimp::Blender::Tex::Type>(temp);
//        ReadFieldPtr<ErrorPolicy_Warn>(dest.ima,"*ima",db);
//
//        db.reader->IncPtr(size);
//    }
//
    fun convertCamera(dest: KMutableProperty0<Camera?>) {

        val d = dest() ?: Camera().also { dest.set(it) }

        readField(Ep.Fail, d.id, "id")
        readField(Ep.Warn, ::tempInt, "type")
        d.type = Camera.Type of tempInt
        readField(Ep.Warn, ::tempInt, "flag")
        d.flag = Camera.Type of tempInt
        readField(Ep.Warn, d::lens, "lens")
        readField(Ep.Warn, d::sensorX, "sensor_x")      /* TODO my current test file does not contain this.
        This might be because the sensor_x (I think it corresponds to sensor_width in bpy doc) default is 0.0f as it is here
        and therefor does not need to be part of the saved file. I should check that */
        readField(Ep.Igno, d::clipSta, "clipsta")
        readField(Ep.Igno, d::clipEnd, "clipend")

        db.reader.pos += size.i
    }
//
////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<MirrorModifierData> (
//    MirrorModifierData& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadField<ErrorPolicy_Fail>(dest.modifier,"modifier",db);
//        ReadField<ErrorPolicy_Igno>(dest.axis,"axis",db);
//        ReadField<ErrorPolicy_Igno>(dest.flag,"flag",db);
//        ReadField<ErrorPolicy_Igno>(dest.tolerance,"tolerance",db);
//        ReadFieldPtr<ErrorPolicy_Igno>(dest.mirror_ob,"*mirror_ob",db);
//
//        db.reader->IncPtr(size);
//    }
//
////--------------------------------------------------------------------------------
//    template <> void Structure :: Convert<Image> (
//    Image& dest,
//    const FileDatabase& db
//    ) const
//    {
//
//        ReadField<ErrorPolicy_Fail>(dest.id,"id",db);
//        ReadFieldArray<ErrorPolicy_Warn>(dest.name,"name",db);
//        ReadField<ErrorPolicy_Igno>(dest.ok,"ok",db);
//        ReadField<ErrorPolicy_Igno>(dest.flag,"flag",db);
//        ReadField<ErrorPolicy_Igno>(dest.source,"source",db);
//        ReadField<ErrorPolicy_Igno>(dest.type,"type",db);
//        ReadField<ErrorPolicy_Igno>(dest.pad,"pad",db);
//        ReadField<ErrorPolicy_Igno>(dest.pad1,"pad1",db);
//        ReadField<ErrorPolicy_Igno>(dest.lastframe,"lastframe",db);
//        ReadField<ErrorPolicy_Igno>(dest.tpageflag,"tpageflag",db);
//        ReadField<ErrorPolicy_Igno>(dest.totbind,"totbind",db);
//        ReadField<ErrorPolicy_Igno>(dest.xrep,"xrep",db);
//        ReadField<ErrorPolicy_Igno>(dest.yrep,"yrep",db);
//        ReadField<ErrorPolicy_Igno>(dest.twsta,"twsta",db);
//        ReadField<ErrorPolicy_Igno>(dest.twend,"twend",db);
//        ReadFieldPtr<ErrorPolicy_Igno>(dest.packedfile,"*packedfile",db);
//        ReadField<ErrorPolicy_Igno>(dest.lastupdate,"lastupdate",db);
//        ReadField<ErrorPolicy_Igno>(dest.lastused,"lastused",db);
//        ReadField<ErrorPolicy_Igno>(dest.animspeed,"animspeed",db);
//        ReadField<ErrorPolicy_Igno>(dest.gen_x,"gen_x",db);
//        ReadField<ErrorPolicy_Igno>(dest.gen_y,"gen_y",db);
//        ReadField<ErrorPolicy_Igno>(dest.gen_type,"gen_type",db);
//
//        db.reader->IncPtr(size);
//    }

    companion object {
        // workaround for https://youtrack.jetbrains.com/issue/KT-16303
        private var tempAny: Any? = null
        private var tempElemBase: ElemBase? = null
        private var tempInt = 0

        var isElem = false
    }
}