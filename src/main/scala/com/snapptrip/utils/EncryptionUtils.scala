package com.snapptrip.utils

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Paths}
import java.security._
import java.security.cert.{CertificateFactory, X509Certificate}
import java.security.interfaces.RSAPublicKey
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64

import com.snapptrip.utils.EncryptionConfig.loadPublicKeyFromString
import com.typesafe.scalalogging
import com.typesafe.scalalogging.LazyLogging
import javax.crypto.Cipher

import scala.util.Try

object KeyPairs {

  val beta_private_key = "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC3FqOHOBZj5JEC\nP79fwIhtI/BPrxajG3QAwWYVwOcWcCxJBKGI0xG/PlmTsOE2XLXlNQA6/jjBFepK\nfbnOF4N3HC3V+KoClDCLq9vIrkVoQ0wLxkEWZ8dza4H7pWFkmyLlGVvRusqIiTWR\n6aFJqKALftUzxtsCXs7L+Q3ig0K3r00H1VJk9xoyCPyWRHfaxdzrlJ7g1NHi47mA\nov28CdyMYbrcsGX25OXcSSE/1jjYnkEep+FctSRxr8MafOGW0HS5BCgVcX7jb682\nTbteTr8TzNuvJeF6wV3YL3ZSvPJfX3FlHo2Bm37J1yutC54PMZcgjcj9AccFfj6b\nax0pfiM1AgMBAAECggEAFd8hd+TOX+6NGByvauvgIFGbwpki9icwa2qvHEgoEvkh\ngTOJbIPyacsz/j2yGcFnhVMYjPOTqhVJIM72JFWQweje3OHQxXAYgIbuQYH4DqWD\nbyCm49tP60zbgUlXYiJaP84QtvQ+f6MMgDrNprP7MoRbSc0mmpji0WEWNIoDrVeS\ntNepM3Jh/suHNAS+x4HqTdVAaeL7GYmfiE3mYKYaCTlzHI0B8anF6BmCJSkPPyWx\n9LPQqIJRBJR5SjlnRu6djkWhjlyhlDjeyIVjrTrTKN3qF4GtYFCyIGKkoaIfgX1S\n5PAgqxDJimQwb355HJgFU6qur/rFT7qOe1LDWQ6Y3QKBgQDwYAzsX5y+hTQfJLUb\n7Pm/ZcVcbb6OdabESTl7VhCLufG8nC12Tu7Arh5KqWpO+xLlheS72vz6hVItzK1s\nDNci3TKxosM/PsY/hPBNHSy1FtHQqQWXQP0i1Gb3QBX/4GuY+Ni5Gf1kQT1pn566\nPh2aiRF9cY/cqOQa/0ezUzaLywKBgQDC/U+sMHa58Lrm8OyOxHnReSMPEzNmCbYr\nqZ2LNYf1c1VEZCZATW1pjtgzM6n+2fMK8rl9EFpdsGmOJxrYtDEWlEROlpP16XIc\nCUEKohoD2dOsxZYCjMELVkpHJgz2oO6qmmkG0fjr5ik/IuD76eRpjgkFDo2A/Un8\ns70ssiYs/wKBgQDmQmw+xbxFuR3bs3MWBCSdluS/DJWcq9ELeEVrA1+oKYAKt+TI\nzwXgZyq3RKCEdfh9C4y38yeg57N4EfgSaB5x1EiqZwr3e6+2yybSB996vRhR9IdZ\ng7S495SGxsoMKbOIfuBEOERGFES6+F+5cqUzEphBfVZc/yA7SE0r18uJjwKBgQC6\nBONPR+TcOrZzv4BOK05z8nbp2M1+GOG8AdKUjfsoxGCiIFEFo1gwDeBf6HfS86YT\n2b/vefxbyQLKZLAN8Mmr2sLXnnuFbLI54PI17LOY3OQ9vToBMLqx0B6Ihdp0Js/J\n3gk4QnSQyECtRN8SNO59bM1aD6obVoiRFf/xdftJjQKBgCXhTPswj4QRkqiyntX5\nbC2qDZtp0wrGLYNsvVH1DoMibOzrklAPJYqtBOD3oiu4vuYJSSLqTrGWcX+J1LuK\nyISTKKWL6BWpkithYWwvdnGko7NncJFtX6v2cyo+Z4E4itgd81CVilk14836NsJq\nw9BbaohXkrUIBGKsMvn7Z1zu\n-----END PRIVATE KEY-----"
  val example_key = "-----BEGIN CERTIFICATE-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtxajhzgWY+SRAj+/X8CI\nbSPwT68Woxt0AMFmFcDnFnAsSQShiNMRvz5Zk7DhNly15TUAOv44wRXqSn25zheD\ndxwt1fiqApQwi6vbyK5FaENMC8ZBFmfHc2uB+6VhZJsi5Rlb0brKiIk1kemhSaig\nC37VM8bbAl7Oy/kN4oNCt69NB9VSZPcaMgj8lkR32sXc65Se4NTR4uO5gKL9vAnc\njGG63LBl9uTl3EkhP9Y42J5BHqfhXLUkca/DGnzhltB0uQQoFXF+42+vNk27Xk6/\nE8zbryXhesFd2C92UrzyX19xZR6NgZt+ydcrrQueDzGXII3I/QHHBX4+m2sdKX4j\nNQIDAQAB\n-----END CERTIFICATE-----"

}

object EncryptionConfig extends EncryptionUtilsComponent with LazyLogging {

  val private_key: PrivateKey = loadPrivateKeyFromString(KeyPairs.beta_private_key)
  val example_key: PublicKey = loadPublicKeyFromString(KeyPairs.example_key)

  def loadKeys(): Unit = {
    println(s"private key loaded :${private_key != null}")
    println(s"private key loaded :${example_key.toString}")
  }

}

trait EncryptionUtilsComponent extends scalalogging.LazyLogging {


  /**
    * openssl genrsa -out private.key 2048
    * openssl req -new -x509 -key private.key -out publickey.cer
    * openssl pkcs12 -export -out public_privatekey.pfx -inkey private.key -in publickey.cer
    *
    * openssl pkcs8 -topk8 -in private.key -inform pem -out private_key_pkcs8.pem -outform pem -nocrypt
    *
    * https://stackoverflow.com/questions/16480846/x-509-private-public-key
    */

  protected def loadPrivateKeyFromString(key: String): PrivateKey = {
    Try {
      val cleanPrivateKeyContent =
        key.replaceAll("""\n""", "")
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .mkString("")
      val kf = KeyFactory.getInstance("RSA")
      val keySpecPKCS8: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder.decode(cleanPrivateKeyContent))
      val privateKey = kf.generatePrivate(keySpecPKCS8)
      privateKey
    }.toEither match {
      case Right(value) => value
      case Left(e) => logger.error("error while loading private key", e)
        null
    }
  }

  /**
    * @param fileName : "private_beta_key_pkcs8.pem"
    * @return
    */
  protected def loadPrivateKey(fileName: String): PrivateKey = {
    Try {
      val privateKeyContent =
        Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(fileName).toURI))
          .map(_.toChar).mkString
      val cleanPrivateKeyContent = privateKeyContent.replaceAll("\\n", "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
      val kf = KeyFactory.getInstance("RSA")
      val keySpecPKCS8: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder.decode(cleanPrivateKeyContent))
      val privateKey = kf.generatePrivate(keySpecPKCS8)
      privateKey
    }.toEither match {
      case Right(value) => value
      case Left(e) => logger.error("error while loading private key", e)
        null
    }
  }


  def loadPublicKey(key: String): PublicKey = {
    Try {
      val certificateFactory = CertificateFactory.getInstance("X.509")
      val inputStream = new ByteArrayInputStream(key.getBytes)
      val certificate = certificateFactory.generateCertificate(inputStream).asInstanceOf[X509Certificate]
      val publicKey: PublicKey = certificate.getPublicKey
      publicKey
    }.toEither match {
      case Right(value) => value
      case Left(e) =>
        logger.error("error while loading pubic key", e)
        null
    }
  }

  def loadPublicKeyFromString(key: String, algorithm: String = "RSA"): PublicKey = {
    Try {
      val cleanPublicKeyContent = key
        .replaceAll("""\n""", "")
        .replace("-----BEGIN CERTIFICATE-----", "")
        .replace("-----END CERTIFICATE-----", "").mkString("")
      val kf = KeyFactory.getInstance(algorithm)
      val keySpecX509: X509EncodedKeySpec = new X509EncodedKeySpec(Base64.getDecoder.decode(cleanPublicKeyContent))
      val publicKey: RSAPublicKey = kf.generatePublic(keySpecX509).asInstanceOf[RSAPublicKey]
      publicKey
    }.toEither match {
      case Right(value) => value
      case Left(e) =>
        logger.error("error while loading public key", e)
        null
    }
  }

  def encryptWithDigest(privateKey: PrivateKey, message: String, algorithm: String = "SHA256withRSA", hashAlgo: String = "SHA-256"): Option[(String, String)] = {

    import java.security.MessageDigest
    import java.util.Base64
    Try {

      val digest = MessageDigest.getInstance(hashAlgo).digest(message.getBytes())
      val signer = Signature.getInstance(algorithm)
      signer.initSign(privateKey)
      signer.update(wrapForRsaSign(digest, hashAlgo))

      val signed = signer.sign
      Base64.getEncoder.encodeToString(signed)

      (Base64.getEncoder.encodeToString(signed), Base64.getEncoder.encodeToString(digest))

    }.toOption
  }

  def verifyWithDigest(publicKey: PublicKey, message: String, signature: String, algorithm: String = "SHA256withRSA"): Boolean ={
    Try {
      val verifier = Signature.getInstance(algorithm)
      verifier.initVerify(publicKey)
      verifier.update(signature.getBytes)
      val privateSignature = Base64.getDecoder.decode(message)
      verifier.verify(privateSignature)
    }.toEither match {
      case Right(true) => true
      case Right(false) => false
      case Left(e) =>
        logger.info("error while verifying message signature", e)
        false
    }
  }

  import java.io.IOException
  import java.security.DigestException

  import org.bouncycastle.asn1.{ASN1Encodable, DERNull, DEROctetString, DERSequence}
  import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder

  private def wrapForRsaSign(dig: Array[Byte], hashAlgo: String) = {
    val oid = new DefaultDigestAlgorithmIdentifierFinder().find(hashAlgo).getAlgorithm
    val oidSeq = new DERSequence(Array[ASN1Encodable](oid, DERNull.INSTANCE))
    val seq = new DERSequence(Array[ASN1Encodable](oidSeq, new DEROctetString(dig)))
    try
      seq.getEncoded
    catch {
      case e: IOException =>
        throw new DigestException(e)
    }
  }

  def encrypt(privateKey: PrivateKey, message: String, algorithm: String = "SHA256withRSA"): Option[String] = {
    Try {
      val cipher = Cipher.getInstance(algorithm)
      cipher.init(Cipher.ENCRYPT_MODE, privateKey)
      val encrypted = cipher.doFinal(message.getBytes())
      Base64.getEncoder.encodeToString(encrypted)
    }.toOption
  }

  def decrypt(publicKey: PublicKey, encrypted: Array[Byte], algorithm: String = "SHA256withRSA"): String = {
    val message = Base64.getDecoder.decode(encrypted)
    val cipher: Cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.DECRYPT_MODE, publicKey)
    val result = cipher.doFinal(message)
    result.map(_.toChar).mkString
  }

  def sign(privateKey: PrivateKey, message: String, algorithm: String = "SHA256withRSA"): Option[String] = {
    Try {
      val privateSign = Signature.getInstance(algorithm)
      privateSign.initSign(privateKey)
      privateSign.update(message.getBytes("UTF-8"))
      val signatureBytes: Array[Byte] = privateSign.sign()
      Base64.getEncoder.encodeToString(signatureBytes)
    }.toOption
  }

  def verify(publicKey: PublicKey, message: String, signature: String, algorithm: String = "SHA256withRSA"): Boolean = {
    Try {
      val publicSignature = Signature.getInstance(algorithm)
      publicSignature.initVerify(publicKey)
      publicSignature.update(message.getBytes("UTF-8"))
      val privateSignature = Base64.getDecoder.decode(signature)
      publicSignature.verify(privateSignature)
    }.toEither match {
      case Right(true) => true
      case Right(false) => false
      case Left(e) =>
        logger.info("error while verifying message signature", e)
        false
    }
  }


}


object SampleKeyPairs extends EncryptionUtilsComponent {

  /**
    * openssl genrsa -out private.key 2048
    * openssl req -new -x509 -key private.key -out publickey.cer
    * openssl pkcs12 -export -out public_privatekey.pfx -inkey private.key -in publickey.cer
    *
    * openssl pkcs8 -topk8 -in private.key -inform pem -out private_key_pkcs8.pem -outform pem -nocrypt
    *
    * https://stackoverflow.com/questions/16480846/x-509-private-public-key
    */
  private val ap_b2b_public_key =
    """-----BEGIN CERTIFICATE-----
      |MIIDVDCCAjygAwIBAgIEW8M49DANBgkqhkiG9w0BAQsFADBsMQswCQYDVQQGEwJJ
      |UjEPMA0GA1UECAwGVGVocmFuMQ8wDQYDVQQHDAZGbGlnaHQxFTATBgNVBAoMDEFz
      |YW5QYXJkYWtodDEPMA0GA1UECwwGTW9iaWxlMRMwEQYDVQQDDApBc2FuRmxpZ2h0
      |MB4XDTE4MTAxNDEyMzkxNloXDTE5MTAxNDEyMzkxNlowbDELMAkGA1UEBhMCSVIx
      |DzANBgNVBAgMBlRlaHJhbjEPMA0GA1UEBwwGRmxpZ2h0MRUwEwYDVQQKDAxBc2Fu
      |UGFyZGFraHQxDzANBgNVBAsMBk1vYmlsZTETMBEGA1UEAwwKQXNhbkZsaWdodDCC
      |ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOL09fm5uN+nJf24gIwfdvSn
      |G8x1rvpH8blOh5kQeJ0EWr3tw6jDu0Os8mrHPnNSNCKMvpZW4Yvi0GZrhmblQ+oV
      |Qy7UBKqF7zo/XcsNp3VtpLlx8bgr40OxAuvV08dyHDz2sKFIxdl/vSLYvyypOw1f
      |ft0SDLYcwkzZKX5zxhk/XSkOWdhkXcmLDcTqWhH73Cj1hrcbMtRthw1wmxJhUXua
      |4X+iPBO6MHl28YDgrARMrwc6mYVthd8Q/2HC2FHkob89Qd/jSLPs5dshdi19n/Bo
      |4EiH+liQN4uA9i1anDpXRdfqh5MatRb6O/PtFe66o9QCqMzqU3giVvURx1QUTiUC
      |AwEAATANBgkqhkiG9w0BAQsFAAOCAQEAVyEgvPKhkS3wT2iNPvVapJ3aORfta2Ja
      |yHk6fjs3Qv41wTqbmxaXWdCzRn8aUaXvP0VxGj3Nr7UqGFLJ11YJ1oJ3jtXAFYA3
      |oREZ679V1Ko7A3pRx67mY4bRU5k7vZ96jpmX4kAxggTmr6mzKQLhIUlIQBNSCNFK
      |W7dizdLnUmVxthyZU/iVAdyK1dXH2FJH2cfq00Z8zFhIP2Z5slXJutrmbA/00f/V
      |JgKYyG2ZyXcNxJM0YjY9XiIgrRJYAeJjSJD/8wrSqI5QfFIs3FekCaq2X5vnV2N3
      |4a+O2uf8xPWVUFukQy/Ai9wNAWuiMfqrKHxNAsgzvGUlaOXyfOauCA==
      |-----END CERTIFICATE-----""".stripMargin

  println(loadPublicKey(ap_b2b_public_key))

  private val publicKeyString: String =
    """-----BEGIN CERTIFICATE-----
      |MIIDXTCCAkWgAwIBAgIJALLvIz/aVygvMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV
      |BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX
      |aWRnaXRzIFB0eSBMdGQwHhcNMTgwOTE5MjA1NDAwWhcNMTgxMDE5MjA1NDAwWjBF
      |MQswCQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50
      |ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB
      |CgKCAQEAtZRrimbUxeEL3WUNHH2pcE5JoHFiDS8CAWpGlzd1CaI3Ml6i54l04XO8
      |uWen+EgWuSN7oSMUnE56g8mNsttLT640x5DDf61NcKh57cr2QBibc+z2f4PL2+Mr
      |Kqo8JMwHd31KU3EVQkiA08gLNwnU/PSPlzIbi8T7sUIrNpK9sE2I2WH7LLoV/aXz
      |NRW1hVVIWLp/shMgZEHj0jpKMsqUjL0ZNv674LML6MMErlbGb2RIPOVCQyDjpxt4
      |xysQRgWB1lJdqoFnVa2JU/3CgQoe4Ogr1die/xX8W02+DoXI2YAL3D2rUqLG6IaM
      |589QlVBNOdTZODyfdpq04e2RPK5DcwIDAQABo1AwTjAdBgNVHQ4EFgQU3WScz9vB
      |s2mwNzeC/NCVGMtQ19QwHwYDVR0jBBgwFoAU3WScz9vBs2mwNzeC/NCVGMtQ19Qw
      |DAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEARcP7pbRc6wyGPWuNU7ux
      |//8ySs/2YCybfOhrFtcnegsasBef960yDAL8/JI6/VQM/7aUR16spNIQ6CFIftpA
      |mSJXK71fBWk5raP58p3yC9o+5px89WquHuYchQ4HoCwdF51tEyqLqoLM5eJbQBos
      |ml5LiOuUXNJoH7rxDHl1Ca+Sxj9OBXAqbqhBlQBym1J3vX76KC48/dbdYCwa0EKZ
      |ub8rloK3jKk5WMc7K6ic2JpTgL0wDX7QqYSKIJfDAnC1ZbreDGEuj4XSpXmcyi2n
      |n+ziLZsMJOuwCGfh3rtfGYRSd1YbAZgUPn9csSRECrp0x8s7J9K3PV4mCznWRD1B
      |GQ==
      |-----END CERTIFICATE-----""".stripMargin

  private val testPublicKeyString =
    """-----BEGIN CERTIFICATE-----
      |MIIDXTCCAkWgAwIBAgIJALLvIz/aVygvMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV
      |BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX
      |aWRnaXRzIFB0eSBMdGQwHhcNMTgwOTE5MjA1NDAwWhcNMTgxMDE5MjA1NDAwWjBF
      |MQswCQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50
      |ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB
      |CgKCAQEAtZRrimbUxeEL3WUNHH2pcE5JoHFiDS8CAWpGlzd1CaI3Ml6i54l04XO8
      |uWen+EgWuSN7oSMUnE56g8mNsttLT640x5DDf61NcKh57cr2QBibc+z2f4PL2+Mr
      |Kqo8JMwHd31KU3EVQkiA08gLNwnU/PSPlzIbi8T7sUIrNpK9sE2I2WH7LLoV/aXz
      |NRW1hVVIWLp/shMgZEHj0jpKMsqUjL0ZNv674LML6MMErlbGb2RIPOVCQyDjpxt4
      |xysQRgWB1lJdqoFnVa2JU/3CgQoe4Ogr1die/xX8W02+DoXI2YAL3D2rUqLG6IaM
      |589QlVBNOdTZODyfdpq04e2RPK5DcwIDAQABo1AwTjAdBgNVHQ4EFgQU3WScz9vB
      |s2mwNzeC/NCVGMtQ19QwHwYDVR0jBBgwFoAU3WScz9vBs2mwNzeC/NCVGMtQ19Qw
      |DAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEARcP7pbRc6wyGPWuNU7ux
      |//8ySs/2YCybfOhrFtcnegsasBef960yDAL8/JI6/VQM/7aUR16spNIQ6CFIftpA
      |mSJXK71fBWk5raP58p3yC9o+5px89WquHuYchQ4HoCwdF51tEyqLqoLM5eJbQBos
      |ml5LiOuUXNJoH7rxDHl1Ca+Sxj9OBXAqbqhBlQBym1J3vX76KC48/dbdYCwa0EKZ
      |ub8rloK3jKk5WMc7K6ic2JpTgL0wDX7QqYSKIJfDAnC1ZbreDGEuj4XSpXmcyi2n
      |n+ziLZsMJOuwCGfh3rtfGYRSd1YbAZgUPn9csSRECrp0x8s7J9K3PV4mCznWRD1B
      |GQ==
      |-----END CERTIFICATE-----""".stripMargin

  loadPublicKey(testPublicKeyString)

  private val privateKeyString: String =
    """-----BEGIN PRIVATE KEY-----
      |MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC1lGuKZtTF4Qvd
      |ZQ0cfalwTkmgcWINLwIBakaXN3UJojcyXqLniXThc7y5Z6f4SBa5I3uhIxScTnqD
      |yY2y20tPrjTHkMN/rU1wqHntyvZAGJtz7PZ/g8vb4ysqqjwkzAd3fUpTcRVCSIDT
      |yAs3CdT89I+XMhuLxPuxQis2kr2wTYjZYfssuhX9pfM1FbWFVUhYun+yEyBkQePS
      |OkoyypSMvRk2/rvgswvowwSuVsZvZEg85UJDIOOnG3jHKxBGBYHWUl2qgWdVrYlT
      |/cKBCh7g6CvV2J7/FfxbTb4OhcjZgAvcPatSosbohoznz1CVUE051Nk4PJ92mrTh
      |7ZE8rkNzAgMBAAECggEAJ0mpyxRczUPYMVr/7z4xPx9xRsnBkWCuamf4Rhe1txPm
      |JXvce4R+SZmlJ+iJq2rNn9lRkpfoiblhQMqHYj+Yl/D1coicBDAXJV0OpKjZhxzL
      |5dVLsGNv5G/OkziqMrwCkZQGVO+OzfyqAyixZIM4FlNEDk7FLwS8AKp0dyZg7Jg8
      |yyy3HUpUyV3wh+K3eDiFvH1uXhEgPrxdPeUhlTXvEzPdv8dLn68L5anpSv+jrz/8
      |CbkfUDb6IbkGIKu5kXtN1jrsK+EIBRWVdtcYjz0rAlkoJcuW6VFgvSAkuAiim3ax
      |DZWRwvxYYeGBJPkeqJB3AXFyjyXG1QDH61gvDnbzgQKBgQDX/M/xibK70GACfifV
      |O0QFLyGmodv55zEcpm5MpEIhPyTleEeGHKEXALT2KcJjn+XbQi3hBaMLCiiRWApn
      |hMXNZKeXQF7s1Q+6wSP6yDChuNkRrBpv+pDNRESSig551TuLEX8PXRbpSIQY1k82
      |h+yjpILNJoujFL9sieo2RQxA8QKBgQDXN9J72AILCQtpkYkmEU6b98i/Jb6VMhiT
      |26sR839Yoq6kxFRb/FliXuMbpPyv9J42f7bbCq77Vee2qKYkpynojhsY57Gb+MCR
      |ePzjHxLuUz19sYK9DAZ7l4++ivDeuLU4bZzlm5n1fOEbohrdJSnmEIKTPmLQbPwN
      |IDCEqM+KowKBgClvsgEXtrd7cX/o7gFlflY0RZNvSAF4jh59+3kuphU0xQJVAfvD
      |sE/2bcEwH7/3JCTdXGnoJ/BtQX0o084qVbxizBgjXFK5SWw9s4ZgM5xDFznht6y1
      |+GO47iLi44YSF3tFnwe2hze4FsehDc9bYlW+sO1ksLNXvqW3C7zfoEsxAoGBAKx0
      |c7p/LavwFHAEibSyW55JIFhjA2OEIAOjG2KoDpxUx8MJ+1s2S92ykSOewkiwqHKd
      |RkYhOnP1s1Y/r9phBQjvjjEXv89utcgb/fB0/vNwSi7FJjEcSLmSikGyDi86LSTJ
      |WNb4J5d5+Nvuur4IQJm1Exyv+fhvzE0sRRSYPK4nAoGAT28CIN/72VmP+xJTxxu3
      |tj1d+0Ruqoglt2phLQVOjNGM9UWSDccTAxA06iUDWyYZz6j4MlMU8X2m1yWSJ0ZZ
      |p2IyNzT+q4AGqweogy0KDXZJk1ssBVK4VjG0nhlEy+kRnaeduGZAETvz5CZjb7mB
      |8uyp9I2DEjeaRO4R/dxK7NM=
      |-----END PRIVATE KEY-----""".stripMargin

  val publicKey: PublicKey = loadPublicKey(publicKeyString)
  val privateKey: PrivateKey = loadPrivateKeyFromString(privateKeyString)

  private val signature: String = sign(privateKey, "11:328seiwn3r283uwi3ur923").get
  println(signature)
  println(s"test sample key pairs result:${verify(publicKey, "11:328seiwn3r283uwi3ur923", signature)}")
}
