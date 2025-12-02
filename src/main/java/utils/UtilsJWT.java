/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.SignatureException;
import io.vertx.core.json.Json;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.*;

import io.vertx.core.json.JsonObject;
import models.ModelSesion;

import javax.print.DocFlavor;

/**
 *
 * @author Ulises Beltrán Gómez --- beltrangomezulises@gmail.com
 */
public class UtilsJWT {

    private static final String STRING_KEY = "k$5*t;ht^L$_g76k'H6LSas\"n`6xrE=)?)+g!~0r198(\"D^|Hl'~+SvuMm'P_([";
    private static final String RECOVER_PRIVATE_KEY = "5]yM#;jbI)=s&!:Lh.:LPwv+~W]GH&_a8J[e*xY}0i8YywNz6<`J'+)hGs'2Z[U46w'wK2+i`!CaXOW#]TGquiF:HS:^M}>~b6xuF_s53N~i#B=VHJO+kBznBdkuDF9FBCCA13757B338279EDE56D1DF3EDCCB23BE6748729257D9F791DCD6A6554B361EBC99B";

    private static String PUBLIC_TOKEN = "EMPTY_TOKEN";
    private static PrivateKey PRIVATE_KEY = null;
    private static final int LAST_CHARS = 128;

    private static Base64.Decoder decoder = Base64.getUrlDecoder();

    public static String generateSessionToken(final int userId, final int branchofficeId) throws JsonProcessingException {
        JwtBuilder builder = Jwts.builder();
        ModelSesion modelSesion = new ModelSesion(1, 1);
        builder.setSubject(Json.encode(modelSesion));
        return builder.signWith(SignatureAlgorithm.HS512, STRING_KEY).compact();
    }

    public static synchronized String getPublicToken() {
        if (isTokenValid(PUBLIC_TOKEN)) {
            return PUBLIC_TOKEN;
        } else {
            JwtBuilder builder = Jwts.builder();
            builder.setSubject("0");
            builder.setIssuer("auth system");
            builder.setIssuedAt(new Date());
            Date expDate = new Date(System.currentTimeMillis() + (1000 * 60 * 60));
            builder.setExpiration(expDate);
            PUBLIC_TOKEN = builder.signWith(SignatureAlgorithm.HS512, STRING_KEY).compact();
            return PUBLIC_TOKEN;
        }
    }

    public static synchronized String generateHash() {
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(new Random(System.currentTimeMillis()).toString());
        builder.setIssuer("auth system");
        builder.setIssuedAt(new Date());
        Date expDate = new Date(System.currentTimeMillis() + (1000 * 60 * 60));
        builder.setExpiration(expDate);
        String hash = builder.signWith(SignatureAlgorithm.ES256, getPrivateKey()).compact();
        return hash.substring(hash.length() - LAST_CHARS);
    }

    private static synchronized PrivateKey getPrivateKey() {
        if (PRIVATE_KEY != null) {
            return PRIVATE_KEY;
        }

        try {
            KeyPairGenerator kpGen = KeyPairGenerator.getInstance("EC");
            kpGen.initialize(new ECGenParameterSpec("secp384r1"));
            KeyPair ecKP = kpGen.generateKeyPair();
            PRIVATE_KEY = ecKP.getPrivate();
            return PRIVATE_KEY;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return PRIVATE_KEY;
    }

    public static int getUserIdFrom(String token) {
        JsonObject payload = getData(token).getJsonObject("payload");
        return Integer.valueOf(payload.getString("sub"));
    }

    private static JsonObject getData(String token) {
        try {
            String[] parts = token.split("\\.");
            JsonObject payload = new JsonObject(new String(decoder.decode(parts[1])));
            JsonObject headers = new JsonObject(new String(decoder.decode(parts[0])));
            return new JsonObject().put("headers", headers)
                    .put("payload", payload);
        } catch (Exception e) {
            e.printStackTrace();
            return new JsonObject().put("headers", new JsonObject())
                    .put("payload", new JsonObject());
        }

    }

    public static boolean isTokenValid(String token) {
        try {
            Jwts.parser().setSigningKey(STRING_KEY).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException
                | UnsupportedJwtException | IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Generates a jwt for refresh a access token
     *
     * @param employeeId id of the user requesting token
     * @return Refresh JWT as string
     */
    public static String generateRefreshToken(final int employeeId) {
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(String.valueOf(employeeId));
        builder.setIssuer("auth system");

        Map<String, Object> claims = new HashMap<>();
        claims.put("time", System.currentTimeMillis() + (1000 * 60 * 60 * 24));
        claims.put("employeeId", employeeId);
        builder.setClaims(claims);

        return builder.signWith(SignatureAlgorithm.HS512, RECOVER_PRIVATE_KEY).compact();
    }
    public static String generateRefreshTokenAPP(final int employeeId) {
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(String.valueOf(employeeId));
        builder.setIssuer("auth system");

        Map<String, Object> claims = new HashMap<>();
        claims.put("time", System.currentTimeMillis() + (1000 * 60 * 60 * 24*60));
        claims.put("employeeId", employeeId);
        builder.setClaims(claims);

        return builder.signWith(SignatureAlgorithm.HS512, RECOVER_PRIVATE_KEY).compact();
    }

    /**
     * Generates a jwt for access a system
     *
     * @param employeeId id of the user requesting token
     * @return Access JWT as string
     */
    public static JsonObject generateAccessToken(final int employeeId) {
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(String.valueOf(employeeId));
        builder.setIssuer("auth system");
        builder.setIssuedAt(new Date());
        Date expDate = new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 2));
        builder.setExpiration(expDate);
        return new JsonObject()
                .put("token", builder.signWith(SignatureAlgorithm.HS512, STRING_KEY).compact())
                .put("expirationDate", expDate.toInstant().toString());
    }
    public static JsonObject generateAccessTokenInternalApp(final int employeeId) {
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(String.valueOf(employeeId));
        builder.setIssuer("auth system");
        builder.setIssuedAt(new Date());
        Date expDate = new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24));
        builder.setExpiration(expDate);
        return new JsonObject()
                .put("token", builder.signWith(SignatureAlgorithm.HS512, STRING_KEY).compact())
                .put("expirationDate", expDate.toInstant().toString());
    }
    public static JsonObject generateAccessTokenAPP(final int employeeId) {
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(String.valueOf(employeeId));
        builder.setIssuer("auth system");
        builder.setIssuedAt(new Date());
        Date expDate = new Date(System.currentTimeMillis() + (1000 * 60*60*24*60));
        builder.setExpiration(expDate);
        return new JsonObject()
                .put("token", builder.signWith(SignatureAlgorithm.HS512, STRING_KEY).compact())
                .put("expirationDate", expDate.toInstant().toString());
    }
    
    public static JsonObject generateAccessTokenPartner(final int employeeId) {
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(String.valueOf(employeeId));
        builder.setIssuer("auth system");
        builder.setIssuedAt(new Date());
        Date expDate = new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24));
        builder.setExpiration(expDate);
        return new JsonObject()
                .put("token", builder.signWith(SignatureAlgorithm.HS512, STRING_KEY).compact())
                .put("expirationDate", expDate.toInstant().toString());
    }
    /**
     * Generates a jws for the process of recover a employee password
     *
     * @param revocerCode generated recover code that the user has to use
     * @return string with the jws
     */
    public static String generateRecoverPasswordToken(final String revocerCode, final String employeeEmail) {
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(new JsonObject()
                .put("recover_code", revocerCode)
                .put("employee_email", employeeEmail).toString()
        );
        builder.setIssuer("auth system");
        return builder.signWith(SignatureAlgorithm.HS512, RECOVER_PRIVATE_KEY).compact();
    }

    /**
     * Generates a new accessToken refreshing the client session
     *
     * @param refreshToken refresh token provided by the login
     * @param accessToken access token provided by the login
     * @return the new access token
     * @throws Exception any value is no valid for the refreshtoken
     */
    public static JsonObject refreshToken(final String refreshToken, final String accessToken) throws Exception {
        try {
            Jwts.parser().setSigningKey(STRING_KEY).parseClaimsJws(accessToken);
            return new JsonObject().put("token", accessToken);
        } catch (ExpiredJwtException e) {
            long timeDiference = System.currentTimeMillis() - e.getClaims().getExpiration().getTime();
            if (timeDiference > (1000 * 60 * 60)) { //is grater than 1 hour
                throw new Exception("Can't refresh accessToken");
            } else {
                Claims claims = Jwts.parser().setSigningKey(RECOVER_PRIVATE_KEY).parseClaimsJws(refreshToken).getBody();
                int employeeId = Integer.parseInt(claims.get("employeeId").toString());
                long tokenTime = Long.parseLong(claims.get("time").toString());

                int actualEmployeeId = Integer.valueOf(e.getClaims().getSubject());
                long actualTime = System.currentTimeMillis();

                if (employeeId == actualEmployeeId
                        && actualTime < tokenTime) {
                    return generateAccessToken(employeeId);
                } else {
                    throw new Exception("Can't refresh accessToken");
                }
            }
        }
    }
    public static JsonObject refreshTokenAPP(final String refreshToken, final String accessToken) throws Exception {
        try {
            Jwts.parser().setSigningKey(STRING_KEY).parseClaimsJws(accessToken);
            return new JsonObject().put("token", accessToken);
        } catch (ExpiredJwtException e) {
            long timeDiference = System.currentTimeMillis() - e.getClaims().getExpiration().getTime();
            if (timeDiference > (1000 * 60 *60*24*90 )) { //is grater than 1 hour
                throw new Exception("Can't refresh accessToken");
            } else {
                Claims claims = Jwts.parser().setSigningKey(RECOVER_PRIVATE_KEY).parseClaimsJws(refreshToken).getBody();
                int employeeId = Integer.parseInt(claims.get("employeeId").toString());
                long tokenTime = Long.parseLong(claims.get("time").toString());

                int actualEmployeeId = Integer.valueOf(e.getClaims().getSubject());
                long actualTime = System.currentTimeMillis();

                if (employeeId == actualEmployeeId
                        && actualTime < tokenTime) {
                    return generateAccessToken(employeeId);
                } else {
                    throw new Exception("Can't refresh accessToken");
                }
            }
        }
    }

    /**
     * Checks if the accesoToken is valid
     *
     * @param accessToken string token to validate
     * @return true if the accessToken is valid, false otherwise
     */
    public static boolean isAccessTokenValid(String accessToken) {
        try {
            Jwts.parser().setSigningKey(STRING_KEY).parseClaimsJws(accessToken);
            return true;
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException
                | UnsupportedJwtException | IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Checks if the recover code is the same to the code inside recoverToken
     *
     * @param recoverToken generated recover token
     * @param recoverCode generated recover code
     * @return true if the recover code and the recover token matches, false otherwise
     */
    public static RecoverValidation isRecoverTokenMatching(final String recoverToken, final String recoverCode) {
        try {
            String object = Jwts.parser().setSigningKey(RECOVER_PRIVATE_KEY).parseClaimsJws(recoverToken).getBody().getSubject();
            JsonObject body = new JsonObject(object);
            if (body.getString("recover_code").equals(recoverCode)) {
                return new RecoverValidation(true, body.getString("employee_email"));
            }
        } catch (Exception e) {
        }
        return new RecoverValidation(false, null);
    }

    public static class RecoverValidation {

        private boolean valid;
        private String employeeMail;

        public RecoverValidation(boolean valid, String employeeMail) {
            this.valid = valid;
            this.employeeMail = employeeMail;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getEmployeeMail() {
            return employeeMail;
        }

        public void setEmployeeMail(String employeeMail) {
            this.employeeMail = employeeMail;
        }

    }
        
}
