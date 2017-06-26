/**
 * Copyright (c) 2015-2017 Inria
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Contributors:
 * - Christophe Gourdin <christophe.gourdin@inria.fr>
 */
package org.occiware.mart.security;

import org.occiware.mart.security.exceptions.ApplicationSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;

/**
 * Created by cgourdin on 24/05/2017.
 */
public class CertificateManagement {
    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateManagement.class);
    // Default jks path.
    public static String KEYSTORE_PATH = System.getProperty("user.home") + System.getProperty("file.separator") + "keystore.jks";
    public static final String KEYSTORE_PASSWORD = "martserver";
    public static final String KEYSTORE_TYPE = "JKS";


    /**
     * Check if server certificate is already in the jkstore
     * @param host hostname to check
     * @param port the port number
     * @param urlPath the url in string format.
     * @param verifyCert , if true, certificates are checked.
     * @throws ApplicationSecurityException thrown when certificate are invalid.
     */
    public static void checkCertificateFromServer(final String host, final int port, final String urlPath, boolean verifyCert) throws ApplicationSecurityException {

        X509Certificate[] certs = getCertificateFromServer(host, port, verifyCert);

        if (certs != null) {
            // Add certificate to the application keystore.
            KeyStore keyStore = null;
            // if keystore doesnt exist in local, the first time we will import all trusted certificate from the overall system.
            if (!isKeyStoreApplicationExist()) {
                // Create an empty keystore.
                keyStore = createApplicationKeyStore(KEYSTORE_TYPE);
            }

            if (keyStore == null) {
                // search on application directory if the key store is not found.
                keyStore = loadApplicationKeyStoreFromDisk();
            }

            // Check validity, else it must be re-validated.
            int size = certs.length;
            LOGGER.debug("Number of certificates received : " + size);

            int i = 1;
            boolean certFound;

            for (X509Certificate cert : certs) {
                certFound = false;
                try {
                    cert.checkValidity();
                    // The certificate is valid for today.
                    // check if certificate exist on our keystore.
                    Certificate certTmp = keyStore.getCertificate(getCertificateAlias(cert));

                    if (certTmp == null) {
                        // Certificate not found in keystore
                        // It must be added.
                        certFound = false;
                    } else {
                        certFound = true;
                    }

                    // Add the certificate for each entry.
                    if (!certFound) {
                        keyStore.setCertificateEntry(getCertificateAlias(cert), cert);
                        saveKeyStoreToDisk(keyStore);
                    }
                } catch (CertificateExpiredException cee) {
                    LOGGER.warn("Certificate has expired for host : " + host + " for port : " + port + " for path : " + urlPath);
                    throw new ApplicationSecurityException(cee.getMessage(), cee);

                } catch (CertificateNotYetValidException ex) {
                    LOGGER.warn("Certificate is not yet valid for host : " + host + " for port : " + port + " for path : " + urlPath);
                    throw new ApplicationSecurityException(ex.getMessage(), ex);
                } catch (KeyStoreException ex) {
                    throw new ApplicationSecurityException(ex.getMessage(), ex);
                }
            }

        } else {
            throw new ApplicationSecurityException("The server has no certified connection with X509 certificates.");
        }
    }

    /**
     *
     * @param addressHost
     * @param port
     * @param verifyCerts
     * @return
     * @throws ApplicationSecurityException
     */
    public static X509Certificate[] getCertificateFromServer(String addressHost, int port, boolean verifyCerts) throws ApplicationSecurityException {
        InetSocketAddress ia;
        ia = new InetSocketAddress(addressHost, port);
        // boolean bVerifyCerts = false;
        int timeOut = 10000;

        // Get the certificates received from the connection
        X509Certificate[] certs = null;
        String protocol = null;
        String cipherSuite = null;
        SSLSocket ss = null;
        try {

            SSLSocketFactory sf;
            if (verifyCerts) {
                sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            } else {
                SSLContext sc = SSLContext.getInstance("SSL");  // "SSL"
                X509TrustManager[] tm = {new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // Trust anything
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // Trust anything
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }};
                SecureRandom m_rnd = new SecureRandom();
                sc.init(null, tm, m_rnd);
                sf = sc.getSocketFactory();
            }

            ss = (SSLSocket) sf.createSocket();
            ss.setSoTimeout(timeOut);
            ss.connect(ia, timeOut);
            SSLSession sess = ss.getSession();
            certs = (X509Certificate[]) sess.getPeerCertificates();
            protocol = sess.getProtocol();
            cipherSuite = sess.getCipherSuite();
            sess.invalidate();
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            throw new ApplicationSecurityException(e.getMessage(), e);
        } finally {
            if (ss != null && !ss.isClosed()) {
                try {
                    ss.close();
                } catch (IOException e) {
                    // Fail silently.
                }
            }
        }

        if (certs != null && certs.length != 0) {
            LOGGER.debug(ia.getHostName() + ":" + ia.getPort() + "\n" + Arrays.toString(certs) + "\n protocol : " + protocol + "\n " + cipherSuite);
        }
        return certs;
    }

    /**
     * Check if keystore exist for the application.
     *
     * @return true if keystore exist.
     */
    public static boolean isKeyStoreApplicationExist() {
        Path path = Paths.get(KEYSTORE_PATH);
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * Create a new, empty keystore.
     *
     * @param keyStoreType The keystore type to create
     * @return The keystore
     * @throws ApplicationSecurityException
     */
    public static KeyStore createApplicationKeyStore(String keyStoreType)
            throws ApplicationSecurityException {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            saveKeyStoreToDisk(keyStore);
        } catch (IOException | GeneralSecurityException ex) {
            throw new ApplicationSecurityException(ex);

        }
        return keyStore;
    }

    /**
     * Save a keyStore to disk.
     *
     * @param keyStore
     * @return
     * @throws ApplicationSecurityException
     */
    public static KeyStore saveKeyStoreToDisk(KeyStore keyStore) throws ApplicationSecurityException {
        File keyStoreFile = new File(KEYSTORE_PATH);
        char[] cPassword = KEYSTORE_PASSWORD.toCharArray();

        try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fos, cPassword);
        } catch (GeneralSecurityException | IOException ex) {
            throw new ApplicationSecurityException(ex);
        }
        return keyStore;
    }

    /**
     *
     *
     * @return
     * @throws ApplicationSecurityException
     */
    public static KeyStore loadApplicationKeyStoreFromDisk() throws ApplicationSecurityException {
        try {
            FileInputStream fis = new FileInputStream(new File(KEYSTORE_PATH));
            char[] cPassword = KEYSTORE_PASSWORD.toCharArray();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fis, cPassword);
            return keyStore;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            int errorCode = -1;
            String message = "";
            if (ex instanceof FileNotFoundException) {
                message = "file not found on load : " + ((FileNotFoundException) ex).getMessage();
            }
            throw new ApplicationSecurityException(ex);
        }

    }

    /**
     *
     * @param cert
     * @return
     */
    public static String getCertificateAlias(X509Certificate cert) {
        X500Principal subject = cert.getSubjectX500Principal();
        X500Principal issuer = cert.getIssuerX500Principal();

        String sSubjectCN = getCommonName(subject);

        // Could not get a subject CN - return blank
        if (sSubjectCN == null) {
            return "";
        }

        String sIssuerCN = getCommonName(issuer);

        // Self-signed certificate or could not get an issuer CN
        if (subject.equals(issuer) || sIssuerCN == null) {
            // Alias is the subject CN
            return sSubjectCN;
        }
        // else non-self-signed certificate
        // Alias is the subject CN followed by the issuer CN in parenthesis
        return MessageFormat.format("{0} ({1})", sSubjectCN, sIssuerCN);
    }

    /**
     *
     * @param vCompCerts
     * @param cert
     * @return
     * @throws ApplicationSecurityException
     */
    public static X509Certificate[] establishTrust(List<X509Certificate> vCompCerts, X509Certificate cert)
            throws ApplicationSecurityException {
        // For each comparison certificate...
        for (X509Certificate compCert : vCompCerts) {
            // Check if the Comparison certificate's subject is the same as the certificate's issuer
            if (cert.getIssuerDN().equals(compCert.getSubjectDN())) {
                // If so verify with the comparison certificate's corresponding private key was used to sign
                // the certificate
                if (verifyCertificate(cert, compCert)) {
                    // If the keystore certificate is self-signed then a chain of trust exists
                    if (compCert.getSubjectDN().equals(compCert.getIssuerDN())) {
                        return new X509Certificate[]{cert, compCert};
                    }
                    // Otherwise try and establish a chain of trust for the comparison certificate against the
                    // other comparison certificates
                    X509Certificate[] tmpChain = establishTrust(vCompCerts, compCert);
                    if (tmpChain != null) {
                        X509Certificate[] trustChain = new X509Certificate[tmpChain.length + 1];
                        trustChain[0] = cert;
                        System.arraycopy(tmpChain, 0, trustChain, 1, tmpChain.length);
                        return trustChain;
                    }
                }
            }
        }

        // No chain of trust
        return null;
    }

    /**
     *
     * @param signedCert
     * @param signingCert
     * @return
     * @throws ApplicationSecurityException
     */
    public static boolean verifyCertificate(X509Certificate signedCert, X509Certificate signingCert) throws ApplicationSecurityException {
        try {
            signedCert.verify(signingCert.getPublicKey());
        } // Verification failed
        catch (InvalidKeyException | SignatureException ex) {
            return false;
        } // Problem verifying
        catch (GeneralSecurityException ex) {
            throw new ApplicationSecurityException("Certificate verification failed !!! : ", ex);
        }
        return true;
    }


    /**
     *
     * @param name
     * @return
     */
    private static String getCommonName(X500Name name) {
        if (name == null) {
            return null;
        }

        RDN[] rdns = name.getRDNs(BCStyle.CN);
        if (rdns.length == 0) {
            return null;
        }

        return rdns[0].getFirst().getValue().toString();
    }

    /**
     *
     * @param name
     * @return
     */
    private static String getCommonName(X500Principal name) {
        if (name == null) {
            return null;
        }

        return getCommonName(X500Name.getInstance(name.getEncoded()));
    }


}
