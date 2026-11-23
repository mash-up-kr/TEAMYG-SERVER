package parfait.core.appversion.domain

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        // 선행 0 허용 ("01.4.0" → SemVer(1,4,0)). DB에서 관리되는 버전값이므로 엄격한 SemVer 형식 불필요.
        private val FORMAT = Regex("""^\d+\.\d+\.\d+$""")

        fun parse(version: String): SemVer {
            require(FORMAT.matches(version)) { "Invalid version format: $version" }
            val parts = version.split(".").map { it.toInt() }
            return SemVer(parts[0], parts[1], parts[2])
        }
    }
}
