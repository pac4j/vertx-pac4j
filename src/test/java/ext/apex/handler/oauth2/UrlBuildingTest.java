package ext.apex.handler.oauth2;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: jez
 */
public class UrlBuildingTest {
        protected Map<String, String> extractQueryParams(URL url) {
            String query = url.getQuery();
            String[] queryPairs = query.split("&");
            return Arrays.stream(queryPairs)
                    .map(s -> s.split("="))
                    .collect(Collectors.toMap(sa -> sa[0], sa -> sa[1]));
        }
}
