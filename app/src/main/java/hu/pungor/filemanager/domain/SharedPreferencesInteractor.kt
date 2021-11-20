package hu.pungor.filemanager.domain

import com.orhanobut.hawk.Hawk

class SharedPreferencesInteractor {
    companion object {
        const val FIRST_TIME: String = "firstTime"

        fun <T> get(key: String): T {
            return Hawk.get(key)
        }

        fun exists(key: String): Boolean {
            return Hawk.contains(key)
        }

        fun <T> store(key: String, value: T) {
            Hawk.put(key, value)
        }
    }
}