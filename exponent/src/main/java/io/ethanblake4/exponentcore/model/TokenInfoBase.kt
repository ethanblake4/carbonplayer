package io.ethanblake4.exponentcore.model
/**
 * A set of tokens
 */
interface TokenInfoBase {
    val auth: String
    /** System identifier */
    val SID: String?
    /** Cloud push identifier */
    val LSID: String?
    /** Google services enabled for this account */
    val services: List<String>?
    /** user identifying info */
    val Email: String?
    val firstName: String
    val lastName: String
    val GooglePlusUpdate: Int?
}