package win32helpers

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Rectangle

object WinFullscreen {

    // Константы из WinUser (JNA уже их определяет, но на всякий случай явно)
    private const val GWL_STYLE   = -16
    private const val GWL_EXSTYLE = -20

    private const val SWP_FRAMECHANGED = 0x0020
    private const val SWP_SHOWWINDOW   = 0x0040

    // Стили окна (основные)
    private const val WS_CAPTION     = 0x00C00000
    private const val WS_SYSMENU     = 0x00080000
    private const val WS_THICKFRAME  = 0x00040000
    private const val WS_MINIMIZEBOX = 0x00020000
    private const val WS_MAXIMIZEBOX = 0x00010000

    // Расширенные стили
    private const val WS_EX_APPWINDOW = 0x00040000  // Заставляет окно появляться на taskbar и в Alt+Tab

    private val HWND_BOTTOM = HWND(Pointer.createConstant(1L))     // Не используем
    private val HWND_NOTOPMOST = HWND(Pointer.createConstant(-2L)) // Обычный слой
    private val HWND_TOPMOST = HWND(Pointer.createConstant(-1L))   // Не используем — ломаем Alt+Tab

    private fun hwnd(frame: Frame): HWND {
        val pointer = Native.getComponentPointer(frame)
            ?: throw IllegalStateException("Не удалось получить HWND: окно не создано или не видно")
        return HWND(pointer)
    }

    fun enter(frame: Frame) {
        val hwnd = hwnd(frame)

        // 1. Получаем текущие стили
        val style = User32.INSTANCE.GetWindowLong(hwnd, GWL_STYLE)
        val exStyle = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE)

        // 2. Убираем все элементы рамки и заголовка
        val newStyle = style and
                WS_CAPTION.inv() and
                WS_SYSMENU.inv() and
                WS_THICKFRAME.inv() and
                WS_MINIMIZEBOX.inv() and
                WS_MAXIMIZEBOX.inv()

        User32.INSTANCE.SetWindowLong(hwnd, GWL_STYLE, newStyle)

        // 3. Добавляем WS_EX_APPWINDOW — критически важно для taskbar и Alt+Tab
        val newExStyle = exStyle or WS_EX_APPWINDOW
        User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, newExStyle)

        // 4. Растягиваем на весь экран
        val screenBounds = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
            .bounds

        User32.INSTANCE.SetWindowPos(
            hwnd,
            HWND_NOTOPMOST,                 // ← Без TOPMOST — Alt+Tab работает!
            screenBounds.x,
            screenBounds.y,
            screenBounds.width,
            screenBounds.height,
            SWP_FRAMECHANGED or SWP_SHOWWINDOW
        )

        // 5. Принудительно активируем окно
        User32.INSTANCE.SetForegroundWindow(hwnd)
    }

    fun exit(frame: Frame, bounds: Rectangle) {
        val hwnd = hwnd(frame)

        val style = User32.INSTANCE.GetWindowLong(hwnd, GWL_STYLE)
        val exStyle = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE)

        // Возвращаем стандартные стили обычного окна
        val newStyle = style or
                WS_CAPTION or
                WS_SYSMENU or
                WS_THICKFRAME or
                WS_MINIMIZEBOX or
                WS_MAXIMIZEBOX

        User32.INSTANCE.SetWindowLong(hwnd, GWL_STYLE, newStyle)

        // Убираем WS_EX_APPWINDOW (опционально — можно оставить, но лучше убрать для чистоты)
        val newExStyle = exStyle and WS_EX_APPWINDOW.inv()
        User32.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, newExStyle)

        // Восстанавливаем прежние размеры и позицию
        User32.INSTANCE.SetWindowPos(
            hwnd,
            HWND_NOTOPMOST,
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height,
            SWP_FRAMECHANGED or SWP_SHOWWINDOW
        )
    }
}