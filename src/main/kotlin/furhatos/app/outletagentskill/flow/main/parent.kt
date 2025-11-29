// 1. 确保包名与 Greeting.kt 和 Idle.kt 一致
package furhatos.app.outletagentskill.flow

// 2. 引入位于 main 包下的 Idle 状态
import furhatos.app.outletagentskill.flow.main.Idle
import furhatos.flow.kotlin.*

val Parent: State = state {

    onUserEnter(instant = true) {
        when { // "it" is the user that entered
            furhat.isAttendingUser -> furhat.glance(it) // Glance at new users entering
            !furhat.isAttendingUser -> furhat.attend(it) // Attend user if not attending anyone
        }
    }

    onUserLeave(instant = true) {
        when {
            !users.hasAny() -> { // last user left
                furhat.attendNobody()
                // 3. 这里因为上面 import 了 Idle，所以不会报错
                goto(Idle)
            }
            furhat.isAttending(it) -> furhat.attend(users.other) // current user left
            !furhat.isAttending(it) -> furhat.glance(it.head.location) // other user left, just glance
        }
    }
}