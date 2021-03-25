package cloud.pace.plugins.cloudsdk

object EnumUtils {
    fun <T : Enum<*>?> searchEnum(enumeration: Class<T>, search: String): T? {
        val enumConstants = enumeration.enumConstants ?: return null
        for (enum in enumConstants) {
            if (enum == null) continue
            if (enum.name.compareTo(search, ignoreCase = true) == 0) {
                return enum
            }
        }
        return null
    }
}