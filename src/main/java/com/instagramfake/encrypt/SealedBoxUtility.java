package com.instagramfake.encrypt;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class SealedBoxUtility {
    public static final int crypto_box_NONCEBYTES = 24;
    public static final int crypto_box_PUBLICKEYBYTES = 32;
    public static final int crypto_box_MACBYTES = 16;
    public static final int crypto_box_SEALBYTES = (crypto_box_PUBLICKEYBYTES + crypto_box_MACBYTES);

//  libsodium
//  int crypto_box_seal(unsigned char *c, const unsigned char *m,
//            unsigned long long mlen, const unsigned char *pk);


    /**
     * Encrypt in  a sealed box
     *
     * @param clearText      clear text
     * @param receiverPubKey receiver public key
     * @return encrypted message
     * @throws GeneralSecurityException
     */
    public static byte[] crypto_box_seal(byte[] clearText, byte[] receiverPubKey) throws GeneralSecurityException {

        // create ephemeral keypair for sender
        TweetNacl.Box.KeyPair ephkeypair = TweetNacl.Box.keyPair();
        // create nonce
        byte[] nonce = crypto_box_seal_nonce(ephkeypair.getPublicKey(), receiverPubKey);
        TweetNacl.Box box = new TweetNacl.Box(receiverPubKey, ephkeypair.getSecretKey());
        byte[] ciphertext = box.box(clearText, nonce);
        if (ciphertext == null) throw new GeneralSecurityException("could not create box");

        byte[] sealedbox = new byte[ciphertext.length + crypto_box_PUBLICKEYBYTES];
        byte[] ephpubkey = ephkeypair.getPublicKey();
        for (int i = 0; i < crypto_box_PUBLICKEYBYTES; i++)
            sealedbox[i] = ephpubkey[i];

        for (int i = 0; i < ciphertext.length; i++)
            sealedbox[i + crypto_box_PUBLICKEYBYTES] = ciphertext[i];

        return sealedbox;
    }

//  libsodium:
//      int
//      crypto_box_seal_open(unsigned char *m, const unsigned char *c,
//                           unsigned long long clen,
//                           const unsigned char *pk, const unsigned char *sk)

    /**
     * Decrypt a sealed box
     *
     * @param c  ciphertext
     * @param pk receiver public key
     * @param sk receiver secret key
     * @return decrypted message
     * @throws GeneralSecurityException
     */
    public static byte[] crypto_box_seal_open(byte[] c, byte[] pk, byte[] sk) throws GeneralSecurityException {
        if (c.length < crypto_box_SEALBYTES) throw new IllegalArgumentException("Ciphertext too short");

        byte[] pksender = Arrays.copyOfRange(c, 0, crypto_box_PUBLICKEYBYTES);
        byte[] ciphertextwithmac = Arrays.copyOfRange(c, crypto_box_PUBLICKEYBYTES, c.length);
        byte[] nonce = crypto_box_seal_nonce(pksender, pk);

        TweetNacl.Box box = new TweetNacl.Box(pksender, sk);
        byte[] cleartext = box.open(ciphertextwithmac, nonce);
        if (cleartext == null) throw new GeneralSecurityException("could not open box");
        return cleartext;
    }


    /**
     * hash the combination of senderpk + mypk into nonce using blake2b hash
     *
     * @param senderpk the senders public key
     * @param mypk     my own public key
     * @return the nonce computed using Blake2b generic hash
     */
    public static byte[] crypto_box_seal_nonce(byte[] senderpk, byte[] mypk) {
// C source ported from libsodium
//      crypto_generichash_state st;
//
//      crypto_generichash_init(&st, NULL, 0U, crypto_box_NONCEBYTES);
//      crypto_generichash_update(&st, pk1, crypto_box_PUBLICKEYBYTES);
//      crypto_generichash_update(&st, pk2, crypto_box_PUBLICKEYBYTES);
//      crypto_generichash_final(&st, nonce, crypto_box_NONCEBYTES);
//
//      return 0;
        final Blake2b blake2b = Blake2b.Digest.newInstance(crypto_box_NONCEBYTES);
        blake2b.update(senderpk);
        blake2b.update(mypk);
        byte[] nonce = blake2b.digest();
        if (nonce == null || nonce.length != crypto_box_NONCEBYTES)
            throw new IllegalArgumentException("Blake2b hashing failed");
        return nonce;


    }

}
