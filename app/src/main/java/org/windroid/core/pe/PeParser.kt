package org.windroid.core.pe

import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Portable Executable (PE32 / PE32+) binary inspector.
 * Parses headers, sections, and import descriptors to determine architecture,
 * subsystem requirements, and runtime dependencies for Wine & Box64 emulation.
 */
object PeParser {

    private const val DOS_MAGIC = 0x5A4D // 'MZ'
    private const val PE_SIGNATURE = 0x00004550 // 'PE\0\0'
    private const val PE32_MAGIC = 0x10B
    private const val PE32_PLUS_MAGIC = 0x20B

    fun parse(file: File): PeInspectionResult {
        if (!file.exists() || !file.canRead()) {
            return PeInspectionResult(
                fileName = file.name,
                fileSize = 0L,
                isValidPe = false,
                errorMessage = "File does not exist or cannot be read"
            )
        }

        val fileSize = file.length()
        // Read header up to 16MB or entire file for import scanning
        val maxHeaderBytes = minOf(fileSize, 8 * 1024 * 1024L).toInt()
        val bytes = ByteArray(maxHeaderBytes)
        val readBytes = file.inputStream().use { it.read(bytes) }
        if (readBytes < 64) {
            return PeInspectionResult(
                fileName = file.name,
                fileSize = fileSize,
                isValidPe = false,
                errorMessage = "File is too small to contain a valid DOS header"
            )
        }

        return parseBuffer(ByteBuffer.wrap(bytes, 0, readBytes).order(ByteOrder.LITTLE_ENDIAN), file.name, fileSize)
    }

    fun parseBytes(data: ByteArray, fileName: String): PeInspectionResult {
        return parseBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), fileName, data.size.toLong())
    }

    private fun parseBuffer(buffer: ByteBuffer, fileName: String, fileSize: Long): PeInspectionResult {
        try {
            if (buffer.remaining() < 64) {
                return PeInspectionResult(fileName, fileSize, false, "Buffer too small for DOS header")
            }

            val dosMagic = buffer.getShort(0).toInt() and 0xFFFF
            if (dosMagic != DOS_MAGIC) {
                return PeInspectionResult(fileName, fileSize, false, "Invalid DOS header magic (not 'MZ')")
            }

            val peOffset = buffer.getInt(0x3C)
            if (peOffset < 0 || peOffset + 24 > buffer.limit()) {
                return PeInspectionResult(fileName, fileSize, false, "Invalid e_lfanew PE header offset: $peOffset")
            }

            buffer.position(peOffset)
            val peSignature = buffer.getInt()
            if (peSignature != PE_SIGNATURE) {
                return PeInspectionResult(fileName, fileSize, false, "Invalid PE signature (not 'PE\\0\\0')")
            }

            // COFF Header (20 bytes)
            val machine = buffer.getShort().toInt() and 0xFFFF
            val numberOfSections = buffer.getShort().toInt() and 0xFFFF
            val timeDateStamp = buffer.getInt().toLong() and 0xFFFFFFFFL
            buffer.getInt() // PointerToSymbolTable
            buffer.getInt() // NumberOfSymbols
            val sizeOfOptionalHeader = buffer.getShort().toInt() and 0xFFFF
            val characteristics = buffer.getShort().toInt() and 0xFFFF

            val architecture = PeArchitecture.fromMachine(machine)
            val optionalHeaderOffset = peOffset + 24

            if (sizeOfOptionalHeader < 2 || optionalHeaderOffset + sizeOfOptionalHeader > buffer.limit()) {
                return PeInspectionResult(
                    fileName = fileName,
                    fileSize = fileSize,
                    isValidPe = true,
                    architecture = architecture,
                    numberOfSections = numberOfSections
                )
            }

            buffer.position(optionalHeaderOffset)
            val optionalMagic = buffer.getShort().toInt() and 0xFFFF
            val is64Bit = (optionalMagic == PE32_PLUS_MAGIC)

            // Skip Linker versions (2 bytes), SizeOfCode (4), SizeOfInitializedData (4), SizeOfUninitializedData (4)
            buffer.position(optionalHeaderOffset + 16)
            val addressOfEntryPoint = buffer.getInt().toLong() and 0xFFFFFFFFL

            buffer.position(optionalHeaderOffset + (if (is64Bit) 24 else 28))
            val imageBase = if (is64Bit) buffer.getLong() else (buffer.getInt().toLong() and 0xFFFFFFFFL)

            // Subsystem offset
            val subsystemOffset = optionalHeaderOffset + (if (is64Bit) 68 else 68)
            val subsystemCode = if (subsystemOffset + 2 <= buffer.limit()) {
                buffer.getShort(subsystemOffset).toInt() and 0xFFFF
            } else 0
            val subsystem = PeSubsystem.fromCode(subsystemCode)

            // Data directories offset: PE32: 96 bytes, PE32+: 112 bytes
            val rvaAndSizesCountOffset = optionalHeaderOffset + (if (is64Bit) 108 else 92)
            val numberOfRvaAndSizes = if (rvaAndSizesCountOffset + 4 <= buffer.limit()) {
                buffer.getInt(rvaAndSizesCountOffset)
            } else 0

            val dataDirOffset = optionalHeaderOffset + (if (is64Bit) 112 else 96)
            var importTableRva = 0L
            var importTableSize = 0L

            if (numberOfRvaAndSizes >= 2 && dataDirOffset + 16 <= buffer.limit()) {
                importTableRva = buffer.getInt(dataDirOffset + 8).toLong() and 0xFFFFFFFFL
                importTableSize = buffer.getInt(dataDirOffset + 12).toLong() and 0xFFFFFFFFL
            }

            // Parse Section Headers (each is 40 bytes)
            val sectionTableOffset = optionalHeaderOffset + sizeOfOptionalHeader
            val sections = mutableListOf<PeSection>()

            for (i in 0 until numberOfSections) {
                val secOffset = sectionTableOffset + (i * 40)
                if (secOffset + 40 > buffer.limit()) break

                buffer.position(secOffset)
                val nameBytes = ByteArray(8)
                buffer.get(nameBytes)
                val name = String(nameBytes).trimEnd { it == '\u0000' }
                val virtualSize = buffer.getInt().toLong() and 0xFFFFFFFFL
                val virtualAddress = buffer.getInt().toLong() and 0xFFFFFFFFL
                val rawDataSize = buffer.getInt().toLong() and 0xFFFFFFFFL
                val rawDataPointer = buffer.getInt().toLong() and 0xFFFFFFFFL
                buffer.position(secOffset + 36)
                val secCharacteristics = buffer.getInt().toLong() and 0xFFFFFFFFL

                sections.add(
                    PeSection(name, virtualSize, virtualAddress, rawDataSize, rawDataPointer, secCharacteristics)
                )
            }

            // Function to translate RVA to file offset
            fun rvaToFileOffset(rva: Long): Long? {
                for (sec in sections) {
                    if (rva >= sec.virtualAddress && rva < sec.virtualAddress + sec.virtualSize) {
                        return sec.rawDataPointer + (rva - sec.virtualAddress)
                    }
                }
                return null
            }

            // Parse Imported DLLs
            val importedDlls = mutableListOf<String>()
            if (importTableRva > 0 && importTableSize > 0) {
                val importOffset = rvaToFileOffset(importTableRva)
                if (importOffset != null && importOffset < buffer.limit()) {
                    var descOffset = importOffset.toInt()
                    // IMAGE_IMPORT_DESCRIPTOR is 20 bytes
                    while (descOffset + 20 <= buffer.limit()) {
                        val originalFirstThunk = buffer.getInt(descOffset).toLong() and 0xFFFFFFFFL
                        val timeDate = buffer.getInt(descOffset + 4)
                        val forwarderChain = buffer.getInt(descOffset + 8)
                        val nameRva = buffer.getInt(descOffset + 12).toLong() and 0xFFFFFFFFL
                        val firstThunk = buffer.getInt(descOffset + 16).toLong() and 0xFFFFFFFFL

                        if (originalFirstThunk == 0L && firstThunk == 0L && nameRva == 0L) {
                            break // Null descriptor terminates list
                        }

                        val nameFileOffset = rvaToFileOffset(nameRva)
                        if (nameFileOffset != null && nameFileOffset < buffer.limit()) {
                            val sb = StringBuilder()
                            var p = nameFileOffset.toInt()
                            while (p < buffer.limit()) {
                                val b = buffer.get(p)
                                if (b == 0.toByte()) break
                                sb.append(b.toInt().toChar())
                                p++
                            }
                            val dllName = sb.toString().trim()
                            if (dllName.isNotEmpty() && !importedDlls.contains(dllName)) {
                                importedDlls.add(dllName)
                            }
                        }
                        descOffset += 20
                    }
                }
            }

            // Analyze graphics backend and runtime recommendations
            val detectedGraphics = detectGraphics(importedDlls)
            val isDotNet = importedDlls.any { it.equals("mscoree.dll", ignoreCase = true) }
            val requiresBox86 = (architecture == PeArchitecture.I386)
            val requiresBox64 = (architecture == PeArchitecture.AMD64)
            val recommendedWineArch = if (architecture == PeArchitecture.I386) "win32" else "win64"

            val recommendedBoxPreset = when (detectedGraphics) {
                GraphicsBackendRequirement.DIRECTX_12,
                GraphicsBackendRequirement.DIRECTX_10_11 -> "Aggressive Performance"
                GraphicsBackendRequirement.DIRECTX_9,
                GraphicsBackendRequirement.OPENGL -> "Balanced"
                else -> "Safe / Compatibility"
            }

            return PeInspectionResult(
                fileName = fileName,
                fileSize = fileSize,
                isValidPe = true,
                architecture = architecture,
                is64Bit = is64Bit,
                subsystem = subsystem,
                entryPointRva = addressOfEntryPoint,
                imageBase = imageBase,
                numberOfSections = sections.size,
                sections = sections,
                importedDlls = importedDlls,
                detectedGraphics = detectedGraphics,
                requiresBox86 = requiresBox86,
                requiresBox64 = requiresBox64,
                isDotNetAssembly = isDotNet,
                recommendedBox64Preset = recommendedBoxPreset,
                recommendedWinePrefixArch = recommendedWineArch
            )
        } catch (e: Exception) {
            return PeInspectionResult(
                fileName = fileName,
                fileSize = fileSize,
                isValidPe = false,
                errorMessage = "PE Parsing error: ${e.message}"
            )
        }
    }

    private fun detectGraphics(importedDlls: List<String>): GraphicsBackendRequirement {
        val lowerNames = importedDlls.map { it.lowercase() }
        return when {
            lowerNames.any { it == "d3d12.dll" } -> GraphicsBackendRequirement.DIRECTX_12
            lowerNames.any { it in listOf("d3d11.dll", "d3d10.dll", "d3d10_1.dll", "dxgi.dll") } ->
                GraphicsBackendRequirement.DIRECTX_10_11
            lowerNames.any { it == "d3d9.dll" } -> GraphicsBackendRequirement.DIRECTX_9
            lowerNames.any { it in listOf("d3d8.dll", "ddraw.dll") } -> GraphicsBackendRequirement.DIRECTX_8_LEGACY
            lowerNames.any { it == "vulkan-1.dll" } -> GraphicsBackendRequirement.VULKAN
            lowerNames.any { it == "opengl32.dll" } -> GraphicsBackendRequirement.OPENGL
            else -> GraphicsBackendRequirement.GDI_SOFTWARE
        }
    }
}
