package io.pivotal.pde.sample.airline.ui.support;

import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.util.StringUtils;

import java.io.*;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Created by cdelashmutt on 7/7/17.
 */
public class SSLPoolFactoryBean extends PoolFactoryBean {

	private String trustedCert;
	private String privateKey;

	@Override
	public void afterPropertiesSet() throws Exception {
		if(StringUtils.hasText(this.trustedCert) && StringUtils.hasText(this.privateKey)) {
			createTrustStore(this.trustedCert, this.privateKey);
		}
		super.afterPropertiesSet();
	}

	private void createTrustStore(String trustedCert, String privateKey) {
		try {
			//Create a new, empty KeyStore in memory.
			KeyStore trustStore = KeyStore.getInstance("JKS");
			char[] password = "passw0rd".toCharArray();
			trustStore.load(null, password);

			//Load up the cert
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			Certificate cert = cf.generateCertificate(new ByteArrayInputStream(trustedCert.getBytes()));
			trustStore.setCertificateEntry("trusted", cert);

			//Load up the private Key
			StringBuffer privateKeyBase64 = new StringBuffer();
			try(BufferedReader privateKeyReader = new BufferedReader(new StringReader(privateKey))) {
				//Read to just after the "BEGIN..." part of the key
				String line = privateKeyReader.readLine();
				while (!"-----BEGIN PRIVATE KEY-----".equals(line)) {
					line = privateKeyReader.readLine();
				}

				//Grab the next line, and read to the "END..." part
				line = privateKeyReader.readLine();
				while (!"-----END PRIVATE KEY-----".equals(line)) {
					privateKeyBase64.append(line);
					line = privateKeyReader.readLine();
				}
			}
			KeyFactory kf = KeyFactory.getInstance("DSA");

			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
					Base64.getDecoder().decode(privateKeyBase64.toString()));
			PrivateKey pk = kf.generatePrivate(keySpec);

			//Store the private key, with the relation to the trustedCert
			trustStore.setKeyEntry("self", pk, "passw0rd".toCharArray(), new Certificate[] {cert});

			// Store away the keystore.
			File keystoreLocation = new File("trusted.keystore");
			FileOutputStream fos = new FileOutputStream(keystoreLocation.getAbsolutePath());
			trustStore.store(fos, password);
			fos.close();

			//Set up system properties
			System.setProperty("javax.net.ssl.trustStoreType", "jks");
			System.setProperty("javax.net.ssl.keyStoreType", "jks");
			System.setProperty("gemfire.ssl-enabled-components", "cluster,server");
			System.setProperty("gemfire.ssl-default-alias", "self");
			System.setProperty("gemfire.ssl-keystore", keystoreLocation.getAbsolutePath());
			System.setProperty("gemfire.ssl-keystore-password", "passw0rd");
			System.setProperty("gemfire.ssl-truststore", keystoreLocation.getAbsolutePath());
			System.setProperty("gemfire.ssl-truststore-password", "passw0rd");
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public String getTrustedCert() {
		return trustedCert;
	}

	public void setTrustedCert(String trustedCert) {
		this.trustedCert = trustedCert;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}
}
