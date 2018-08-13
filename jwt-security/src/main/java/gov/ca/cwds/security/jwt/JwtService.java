package gov.ca.cwds.security.jwt;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.Header;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import com.nimbusds.jwt.SignedJWT;

/**
 * Created by dmitry.rudenko on 6/30/2017.
 */
public class JwtService {

  public static final String IDENTITY_CLAIM = "identity";

  private KeyProvider keyProvider;
  private JwtConfiguration configuration;

  public JwtService(JwtConfiguration configuration) {
    this.configuration = configuration;
    this.keyProvider = new JCEKSKeyProvider(configuration);
  }

  public String generate(String id, String subject, Map<String, String> customJwtClaimsMap) {
    try {
      final JWTClaimsSet claimsSet = prepareClaims(id, subject, customJwtClaimsMap);
      final SignedJWT signedJWT = sign(claimsSet);
      String token;
      if (configuration.isEncryptionEnabled()) {
        final JWEObject jweObject = encrypt(signedJWT);
        token = jweObject.serialize();
      } else {
        token = signedJWT.serialize();
      }
      return removeHeader(token);
    } catch (Exception e) {
      throw new JwtException(e);
    }
  }

  public String generate(String id, String subject, String identity) throws JwtException {
    final Map<String, String> claimsMap = new HashMap<>();
    claimsMap.put(IDENTITY_CLAIM, identity);
    return generate(id, subject, claimsMap);
  }

  public String validate(String token) throws JwtException {
    try {
      final String tokenWithHeader = addHeader(token);
      SignedJWT signedJWT;
      if (configuration.isEncryptionEnabled()) {
        signedJWT = decrypt(tokenWithHeader);
      } else {
        signedJWT = SignedJWT.parse(tokenWithHeader);
      }

      validateSignature(signedJWT);
      final JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
      validateClaims(claimsSet);
      return claimsSet.getStringClaim(IDENTITY_CLAIM);
    } catch (Exception e) {
      throw new JwtException(e);
    }
  }

  private JWEObject encrypt(SignedJWT signedJWT) throws JwtException {
    try {
      final JWEObject jweObject = new JWEObject(jweHeader(), new Payload(signedJWT));
      jweObject.encrypt(new DirectEncrypter(keyProvider.getEncryptingKey().getEncoded()));
      return jweObject;
    } catch (Exception e) {
      throw new JwtException(e);
    }
  }

  private JWEHeader jweHeader() {
    return new JWEHeader.Builder(JWEAlgorithm.DIR,
        EncryptionMethod.parse(configuration.getEncryptionMethod())).contentType("JWT").build();
  }

  private SignedJWT sign(JWTClaimsSet claimsSet) throws JwtException {
    try {
      final JWSSigner signer = new RSASSASigner(keyProvider.getSigningKey());
      final SignedJWT signedJWT = new SignedJWT(jwsHeader(), claimsSet);
      signedJWT.sign(signer);
      return signedJWT;
    } catch (Exception e) {
      throw new JwtException(e);
    }
  }

  private JWSHeader jwsHeader() {
    return new JWSHeader(JWSAlgorithm.RS256);
  }

  private JWTClaimsSet prepareClaims(String id, String subject, Map<String, String> claimsMap) {
    final long nowMillis = new Date().getTime();
    final Builder builder = new Builder().subject(subject).issueTime(new Date(nowMillis))
        .issuer(configuration.getIssuer())
        .expirationTime(new Date(nowMillis + configuration.getTimeout() * 60 * 1000)).jwtID(id);

    claimsMap.forEach(builder::claim);
    return builder.build();
  }

  private void validateSignature(SignedJWT signedJWT) throws JwtException {
    boolean verified = false;
    try {
      verified =
          signedJWT.verify(new RSASSAVerifier((RSAPublicKey) keyProvider.getValidatingKey()));
    } catch (Exception e) {
      throw new JwtException(e);
    }
    if (!verified) {
      fail();
    }
  }

  private SignedJWT decrypt(String token) throws JwtException {
    try {
      final JWEObject jweObject = JWEObject.parse(token);
      jweObject.decrypt(new DirectDecrypter(keyProvider.getEncryptingKey().getEncoded()));
      return jweObject.getPayload().toSignedJWT();
    } catch (Exception e) {
      throw new JwtException(e);
    }
  }

  private void validateClaims(JWTClaimsSet claims) throws JwtException {
    if ((configuration.getIssuer() != null && !configuration.getIssuer().equals(claims.getIssuer()))
        || new Date().after(claims.getExpirationTime())
        || claims.getClaim(IDENTITY_CLAIM) == null) {
      fail();
    }
  }

  private String removeHeader(String token) {
    return configuration.isHeadless() ? token.substring(token.indexOf('.')) : token;
  }

  private String addHeader(String token) {
    if (configuration.isHeadless()) {
      Header header = configuration.isEncryptionEnabled() ? jweHeader() : jwsHeader();
      return header.toBase64URL().toString() + token;
    }
    return token;
  }

  private void fail() throws JwtException {
    throw new JwtException("Token validation failed");
  }

}
