package se.anatom.ejbca.apply;

import java.beans.Beans;
import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import javax.ejb.CreateException;
import javax.ejb.ObjectNotFoundException;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;

import se.anatom.ejbca.SecConst;
import se.anatom.ejbca.ca.exception.AuthLoginException;
import se.anatom.ejbca.ca.exception.AuthStatusException;
import se.anatom.ejbca.ca.exception.IllegalKeyException;
import se.anatom.ejbca.ca.exception.SignRequestException;
import se.anatom.ejbca.ca.exception.SignRequestSignatureException;
import se.anatom.ejbca.ca.sign.ISignSessionHome;
import se.anatom.ejbca.ca.sign.ISignSessionRemote;
import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.protocol.PKCS10RequestMessage;
import se.anatom.ejbca.util.Base64;
import se.anatom.ejbca.util.CertTools;
import se.anatom.ejbca.util.FileTools;
import se.anatom.ejbca.util.KeyTools;
import se.anatom.ejbca.webdist.cainterface.CAInterfaceBean;
import se.anatom.ejbca.webdist.rainterface.RAInterfaceBean;
import se.anatom.ejbca.webdist.rainterface.UserView;

/**
 * This is a servlet that is used for creating a user into EJBCA and
 * retrieving her certificate.  Supports only POST.
 * This servlet requires authentication of the amdinistrator.
 * <p>
 *   The CGI parameters for requests are the following.
 * </p>
 * <dl>
 * <dt>pkcs10req</dt>
 * <dd>
 *   A PKCS#10 request, mandatory. TODO more on this
 * </dd>
 * <dt>username</dt>
 * <dd>
 *   The username (for EJBCA use only).  Optional, defaults to the DN in
 *   the PKCS#10 request.
 * </dd>
 * <dt>password</dt>
 * <dd>
 *   Password for the user (for EJBCA internal use only).  Optional,
 *   defaults to an empty string.
 *   TODO does this have anything to do with the returned cert?
 * </dd>
 * <dt>entityprofile</dt>
 * <dd>
 *   The name of the EJBCA end entity profile for the user.  Optional,
 *   defaults to the built-in EMPTY end entity profile.
 * </dd>
 * <dt>certificateprofile</dt>
 * <dd>
 *   The name of the EJBCA certificate profile to use.  Optional,
 *   defaults to the built-in ENDUSER certificate profile.
 * </dd>
 * </dl>
 *
 * @author Ville Skytt�
 * @version $Id: AdminCertReqServlet.java,v 1.1 2003-01-29 12:01:03 anatom Exp $
 */
public class AdminCertReqServlet extends HttpServlet {

  private final static Category cat = Category.getInstance(AdminCertReqServlet.class.getName());

  private InitialContext ctx = null;
  ISignSessionHome home = null;

  private final static byte[] BEGIN_CERT =
    "-----BEGIN CERTIFICATE-----".getBytes();
  private final static int BEGIN_CERT_LENGTH = BEGIN_CERT.length;

  private final static byte[] END_CERT =
    "-----END CERTIFICATE-----".getBytes();
  private final static int END_CERT_LENGTH = END_CERT.length;

  private final static byte[] NL = "\n".getBytes();
  private final static int NL_LENGTH = NL.length;

  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);
    try {
      // Install BouncyCastle provider
      Provider p = new org.bouncycastle.jce.provider.BouncyCastleProvider();
      int result = Security.addProvider(p);

      // Get EJB context and home interfaces
      ctx = new InitialContext();
      home = (ISignSessionHome) PortableRemoteObject
        .narrow(ctx.lookup("RSASignSession"), ISignSessionHome.class);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }


  /**
   * Handles PKCS10 certificate request, these are constructed as:
   * <pre><code>
   * CertificationRequest ::= SEQUENCE {
   * certificationRequestInfo  CertificationRequestInfo,
   * signatureAlgorithm          AlgorithmIdentifier{{ SignatureAlgorithms }},
   * signature                       BIT STRING
   * }
   * CertificationRequestInfo ::= SEQUENCE {
   * version             INTEGER { v1(0) } (v1,...),
   * subject             Name,
   * subjectPKInfo   SubjectPublicKeyInfo{{ PKInfoAlgorithms }},
   * attributes          [0] Attributes{{ CRIAttributes }}
   * }
   * SubjectPublicKeyInfo { ALGORITHM : IOSet} ::= SEQUENCE {
   * algorithm           AlgorithmIdentifier {{IOSet}},
   * subjectPublicKey    BIT STRING
   * }
   * </pre>
   *
   * PublicKey's encoded-format has to be RSA X.509.
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    //ServletDebug debug = new ServletDebug(request, response);

    X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    if (certs == null) {
        throw new ServletException("This servlet requires certificate authentication!");
    }

    Admin admin = new Admin(certs[0]);

    byte[] buffer = pkcs10Bytes(request.getParameter("pkcs10req"));
    if (buffer == null) {
      // TODO: abort here, no PKCS#10 received
    }

    RAInterfaceBean rabean = getRaBean(request);

    // Decompose the PKCS#10 request, and create the user.
    PKCS10RequestMessage p10 = new PKCS10RequestMessage(buffer);
    String dn = p10.getCertificationRequest().getCertificationRequestInfo().getSubject().toString();

    String username = request.getParameter("username");
    if (username == null || username.trim().length() == 0) {
      username = dn;
    }
    // need null check here?
    // Before doing anything else, check if the user name is unique and ok.
    username = checkUsername(rabean, username);

    UserView newuser = new UserView();
    newuser.setUsername(username);

    newuser.setSubjectDN(dn);
    newuser.setTokenType(SecConst.TOKEN_SOFT_BROWSERGEN);
    newuser.setAdministrator(false);
    newuser.setKeyRecoverable(false);

    String email = CertTools.getPartFromDN(dn, "E"); // BC says VeriSign
    if (email == null) email = CertTools.getPartFromDN(dn, "EMAILADDRESS");
    // TODO: get values from subject altname, lookup email as well?
    // newuser.setSubjectAltName(...)
    if (email != null) {
      newuser.setEmail(email);
    }

    String tmp = null;

    int eProfileId = SecConst.EMPTY_ENDENTITYPROFILE;
    if ((tmp = request.getParameter("entityprofile")) != null) {
      int reqId = rabean.getEndEntityProfileId(tmp);
      if (reqId == 0) {
        throw new ServletException("No such end entity profile: " + tmp);
      } else {
        eProfileId = reqId;
      }
    }
    // TODO: check that we're authorized to use the profile?
    newuser.setEndEntityProfileId(eProfileId);

    int cProfileId = SecConst.CERTPROFILE_FIXED_ENDUSER;
    if ((tmp = request.getParameter("certificateprofile")) != null) {
      CAInterfaceBean cabean = getCaBean(request);
      int reqId = cabean.getCertificateProfileId(tmp);
      if (reqId == 0) {
        throw new ServletException("No such certificate profile: " + tmp);
      } else {
        cProfileId = reqId;
      }
    }
    // TODO: check that we're authorized to use the profile?
    newuser.setCertificateProfileId(cProfileId);

    // TODO: figure out if we can manage without a password.
    String password = request.getParameter("password");
    if (password == null) password = "";
    newuser.setPassword(password);
    newuser.setClearTextPassword(false);

    try {
      rabean.addUser(newuser);
    } catch (Exception e) {
      throw new ServletException("Error adding user: " + e.toString(), e);
    }

    ISignSessionRemote ss;
    try {
      ss = home.create();
    } catch (CreateException e) {
      throw new ServletException(e);
    }

    byte[] pkcs7;
    try {
      X509Certificate cert =
        (X509Certificate) ss.createCertificate(admin, username, password, p10);
      pkcs7 = ss.createPKCS7(admin, cert);
    } catch (ObjectNotFoundException e) {
      // User not found
      throw new ServletException(e);
    } catch (AuthStatusException e) {
      // Wrong user status, shouldn't really happen.  The user needs to have
      // status of NEW, FAILED or INPROCESS.
      throw new ServletException(e);
    } catch (AuthLoginException e) {
      // Wrong username or password, hmm... wasn't the wrong username caught
      // in the objectnotfoundexception above... and this shouldn't happen.
      throw new ServletException(e);
    } catch (IllegalKeyException e) {
      // Malformed key (?)
      throw new ServletException(e);
    } catch (SignRequestException e) {
      // Invalid request
      throw new ServletException(e);
    } catch (SignRequestSignatureException e) {
      // Invalid signature in certificate request
      throw new ServletException(e);
    }

    cat.debug("Created certificate (PKCS7) for " + username);

    sendNewB64Cert(Base64.encode(pkcs7), response);

  }


  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    cat.debug(">doGet()");
    response.sendError(HttpServletResponse.SC_NOT_FOUND, "The certificate request servlet only handles POST method.");
    cat.debug("<doGet()");
  } // doGet


  private void sendNewB64Cert(byte[] b64cert, HttpServletResponse out)
    throws IOException
  {
    out.setContentType("application/octet-stream");
    out.setHeader("Content-Disposition", "filename=cert.pem");
    out.setContentLength(b64cert.length +
                         BEGIN_CERT_LENGTH + END_CERT_LENGTH + (3 *NL_LENGTH));

    ServletOutputStream os = out.getOutputStream();
    os.write(BEGIN_CERT);
    os.write(NL);
    os.write(b64cert);
    os.write(NL);
    os.write(END_CERT);
    os.write(NL);
    out.flushBuffer();
  }


  /**
   *
   */
  private final static byte[] pkcs10Bytes(String pkcs10)
  {
    if (pkcs10 == null) return null;
    byte[] reqBytes = pkcs10.getBytes();
    byte[] bytes = null;
    try {
      // A real PKCS10 PEM request
      String beginKey = "-----BEGIN CERTIFICATE REQUEST-----";
      String endKey   = "-----END CERTIFICATE REQUEST-----";
      bytes = FileTools.getBytesFromPEM(reqBytes, beginKey, endKey);
    } catch (IOException e) {
      try {
        // Keytool PKCS10 PEM request
        String beginKey = "-----BEGIN NEW CERTIFICATE REQUEST-----";
        String endKey   = "-----END NEW CERTIFICATE REQUEST-----";
        bytes = FileTools.getBytesFromPEM(reqBytes, beginKey, endKey);
      } catch (IOException e2) {
        // IE PKCS10 Base64 coded request
        bytes = Base64.decode(reqBytes);
      }
    }
    return bytes;
  }


  /**
   *
   */
  private final RAInterfaceBean getRaBean(HttpServletRequest req)
    throws ServletException
  {
    HttpSession session = req.getSession();
    RAInterfaceBean rabean = (RAInterfaceBean) session.getAttribute("rabean");
    if (rabean == null) {
      try {
        rabean = (RAInterfaceBean) Beans.instantiate(this.getClass().getClassLoader(), "se.anatom.ejbca.webdist.rainterface.RAInterfaceBean");
      } catch (ClassNotFoundException e) {
        throw new ServletException(e);
      } catch (Exception e) {
        throw new ServletException("Unable to instantiate RAInterfaceBean", e);
      }
      try {
        rabean.initialize(req);
      } catch (Exception e) {
        throw new ServletException("Cannot initialize RAInterfaceBean", e);
      }
      session.setAttribute("rabean", rabean);
    }
    return rabean;
  }


  /**
   *
   */
  private final CAInterfaceBean getCaBean(HttpServletRequest req)
    throws ServletException
  {
    HttpSession session = req.getSession();
    CAInterfaceBean cabean = (CAInterfaceBean) session.getAttribute("cabean");
    if (cabean == null) {
      try {
        cabean = (CAInterfaceBean) Beans.instantiate(this.getClass().getClassLoader(), "se.anatom.ejbca.webdist.cainterface.CAInterfaceBean");
      } catch (ClassNotFoundException e) {
        throw new ServletException(e);
      } catch (Exception e) {
        throw new ServletException("Unable to instantiate CAInterfaceBean", e);
      }
      try {
        cabean.initialize(req);
      } catch (Exception e) {
        throw new ServletException("Cannot initialize CAInterfaceBean", e);
      }
      session.setAttribute("cabean", cabean);
    }
    return cabean;
  }


  /**
   *
   */
  private final String checkUsername(RAInterfaceBean rabean, String username)
    throws ServletException
  {
    if (username != null) username = username.trim();
    if (username == null || username.length() == 0) {
      throw new ServletException("Username must not be empty.");
    }

    String msg = null;
    try {
      if (rabean.userExist(username)) {
        msg = "User '" + username + "' already exists.";
      }
    } catch (Exception e) {
      throw new ServletException("Error checking username '" + username +
                                 ": " + e.toString(), e);
    }
    if (msg != null) {
      throw new ServletException(msg);
    }

    return username;
  }

}
