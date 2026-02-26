package org.example

import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.io.File

val CLSID_FileOpenDialog = Guid.GUID.fromString("{DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7}")
val IID_IFileOpenDialog = Guid.GUID.fromString("{D57C7288-D4AD-4768-BE02-9D969532D960}")
val IID_IShellItem = Guid.GUID.fromString("{43826D1E-E718-42EE-BC55-A1E261C37BFE}")

const val FOS_PICKFOLDERS = 0x00000020
const val FOS_FORCEFILESYSTEM = 0x00000040
const val SIGDN_FILESYSPATH = 0x80058000

class ShellItem(ptr: Pointer) : Unknown(ptr) {

    fun getDisplayName(type: Long, out: PointerByReference): WinNT.HRESULT =
        _invokeNativeObject(5, arrayOf(pointer, type, out), WinNT.HRESULT::class.java)
                as WinNT.HRESULT
}

class FileOpenDialog(ptr: Pointer) : Unknown(ptr) {

    fun show(hwnd: Pointer?): WinNT.HRESULT =
        _invokeNativeObject(3, arrayOf(pointer, hwnd), WinNT.HRESULT::class.java)
                as WinNT.HRESULT

    fun setOptions(options: Int): WinNT.HRESULT =
        _invokeNativeObject(9, arrayOf(pointer, options), WinNT.HRESULT::class.java)
                as WinNT.HRESULT

    fun getOptions(out: IntByReference): WinNT.HRESULT =
        _invokeNativeObject(10, arrayOf(pointer, out), WinNT.HRESULT::class.java)
                as WinNT.HRESULT

    fun getResult(out: PointerByReference): WinNT.HRESULT =
        _invokeNativeObject(20, arrayOf(pointer, out), WinNT.HRESULT::class.java)
                as WinNT.HRESULT

    fun setTitle(title: WString): WinNT.HRESULT =
        _invokeNativeObject(17, arrayOf(pointer, title), WinNT.HRESULT::class.java)
                as WinNT.HRESULT
}

fun pickFolderWindowsNative(): File? {
    Ole32.INSTANCE.CoInitializeEx(
        Pointer.NULL,
        Ole32.COINIT_APARTMENTTHREADED
    )

    val dialogRef = PointerByReference()

    val hr = Ole32.INSTANCE.CoCreateInstance(
        CLSID_FileOpenDialog,
        null,
        WTypes.CLSCTX_INPROC_SERVER,
        IID_IFileOpenDialog,
        dialogRef
    )

    if (hr != WinNT.S_OK) {
        Ole32.INSTANCE.CoUninitialize()
        return null
    }

    val dialog = FileOpenDialog(dialogRef.value)

    val optionsRef = IntByReference()
    dialog.getOptions(optionsRef)
    dialog.setOptions(
        optionsRef.value or
                FOS_PICKFOLDERS or
                FOS_FORCEFILESYSTEM
    )

    dialog.setTitle(WString("Выберите папку"))

    if (dialog.show(null) != WinNT.S_OK) {
        dialog.Release()
        Ole32.INSTANCE.CoUninitialize()
        return null
    }

    val itemRef = PointerByReference()
    dialog.getResult(itemRef)

    val item = ShellItem(itemRef.value)
    val pathRef = PointerByReference()
    item.getDisplayName(SIGDN_FILESYSPATH, pathRef)

    val path = pathRef.value.getWideString(0)

    item.Release()
    dialog.Release()
    Ole32.INSTANCE.CoUninitialize()

    return File(path)
}

