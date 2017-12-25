import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;

public class TopSites {
    protected static final String ACTION_NAME = "TopSites";
    protected static final String RESPONSE_GROUP_NAME = "Country";
    protected static final String SERVICE_HOST = "ats.amazonaws.com";
    protected static final String AWS_BASE_URL = "https://" + SERVICE_HOST + "/?";

    protected static final int NUMBER_TO_RETURN = 500;
    protected static int START_NUMBER = 1;
    protected static int SITE_ID = 1;
    protected static final String DATEFORMAT_AWS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String HASH_ALGORITHM = "HmacSHA256";
	
    protected static List<String> SITES = new ArrayList<String>();

    private String accessKeyId;
    private String secretAccessKey;
    private String countryCode;

    public TopSites(String accessKeyId, String secretAccessKey, String countryCode) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.countryCode = countryCode;
    }

    protected String generateSignature(String data)
        throws java.security.SignatureException {
        String result;
        try {
            // get an hmac key from the raw key bytes
            SecretKeySpec signingKey =
                new SecretKeySpec(secretAccessKey.getBytes(),
                                  HASH_ALGORITHM);

            // get a mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // base64-encode the hmac
            result = Base64.getEncoder().encodeToString(rawHmac);


        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : "
                                         + e.getMessage());
        }
        return result;
    }

    public static String getTimestampFromLocalTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(DATEFORMAT_AWS);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    protected String buildQuery() throws UnsupportedEncodingException {
        String timestamp = getTimestampFromLocalTime(Calendar.getInstance().getTime());

        // TreeMap puts keys in alphabetical order
        Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("Action", ACTION_NAME);
        queryParams.put("ResponseGroup", RESPONSE_GROUP_NAME);
        queryParams.put("AWSAccessKeyId", accessKeyId);
        queryParams.put("Timestamp", URLEncoder.encode(timestamp, "UTF-8"));
        queryParams.put("CountryCode", countryCode);
        queryParams.put("Count", "" + NUMBER_TO_RETURN);
        queryParams.put("Start", "" + START_NUMBER);
        queryParams.put("SignatureVersion", "2");
        queryParams.put("SignatureMethod", HASH_ALGORITHM);

        String query = "";
        boolean first = true;
        for (String name : queryParams.keySet()) {
            if (first)
                first = false;
            else
                query += "&";

            query += name + "=" + queryParams.get(name);
        }

        return query;
    }

    private static void writetofile(List<String> sites) throws Exception {
	
	FileWriter fileWriter = null;
	try {
		fileWriter = new FileWriter("Top_500_sites.csv");
		fileWriter.append("Site");
		fileWriter.append("\n");
		
		//Write sites to the CSV file
		for (int i = 0; i < sites.size(); i++) {
            fileWriter.append(String.format("%s", sites.get(i)));
			SITE_ID++;
			fileWriter.append("\n");
		}
		
		System.out.println("Successfully extracted and stored the top 500 sites from Alexa!");
			
		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {
			
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
			}
			
		}
    }

    private static void parseResponse(InputStream in) throws Exception {
        // Parse the response
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document responseDoc = dbf.newDocumentBuilder().parse(in);

        NodeList responses = responseDoc.getElementsByTagNameNS("*", "Site");

        for (int i = 0; i < responses.getLength(); i++) {
            Element response = (Element) responses.item(i);
            Element node = (Element)
                response.getElementsByTagNameNS("*", "DataUrl").item(0);
            String siteUrl = node.getFirstChild().getNodeValue();
	    SITES.add("" + siteUrl);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.exit(-1);
        }
        String accessKey = args[0];
        String secretKey = args[1];
        String countryCode = (args.length > 2) ? args[2] : "";

        TopSites topSites = new TopSites(accessKey, secretKey, countryCode);
	
	for (int i=0; i<5; i++) {
		String query = topSites.buildQuery();
        String toSign = "GET\n" + SERVICE_HOST + "\n/\n" + query;
        String signature = topSites.generateSignature(toSign);
        String uri = AWS_BASE_URL + query +
		    "&Signature=" + URLEncoder.encode(signature, "UTF-8");
        URL url = new URL(uri);
		URLConnection conn = url.openConnection();
		InputStream in = conn.getInputStream();

		parseResponse(in);
		START_NUMBER += 100;
	
	}
		//Save to CSV
		writetofile(SITES);

    }

}
