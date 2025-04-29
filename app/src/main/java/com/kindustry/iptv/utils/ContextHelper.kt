package com.kindustry.iptv.utils

import android.content.Context
import com.kindustry.iptv.R
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.pkcs.RSAPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import javax.crypto.Cipher
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.Security
import java.security.spec.RSAPrivateKeySpec

object ContextHelper {

    /**
     * 数据解密.
     *
     * @return 原始数据，字符串
     */
    fun decryptBinaryToString(
        context: Context,
        transformation: String = "RSA/ECB/PKCS1Padding", // 加密算法标准算法名称
        charset: Charset = Charsets.UTF_8 // 使用 Charset 类型，并直接赋值 Charsets 的常量
    ): String? {
        return try {
            Security.addProvider(BouncyCastleProvider())
            // 获取 Resources 对象
            val resources = context.resources
            // 确保 R.raw.jp 存在
            resources.openRawResource(R.raw.j).use {
                // 读取 InputStream 的所有字节
                val encryptedBytes = it.readBytes()
                // 替换为你的 DER 私钥文件路径
                resources.openRawResource(R.raw.k).use { inputStream ->
                    ASN1InputStream(inputStream).use { asn1InputStream ->
                        val rsaPrivateKey = RSAPrivateKey.getInstance(asn1InputStream.readObject())
                        val keySpec = RSAPrivateKeySpec(rsaPrivateKey.modulus, rsaPrivateKey.privateExponent)
                        val keyFactory = KeyFactory.getInstance("RSA") // 显式指定 Provider
                        val privateKey = keyFactory.generatePrivate(keySpec)

                        val cipher = Cipher.getInstance(transformation)
                        cipher.init(Cipher.DECRYPT_MODE, privateKey)
                        val decryptedBytes  = cipher.doFinal(encryptedBytes)
                        String(decryptedBytes, charset)
                    }
                }
            }
        } catch (e: Exception) {
            println("Decrypted data Error : ${e.message}")
            e.printStackTrace()
            null
        }
    }



}