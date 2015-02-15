package nordlib.net.imagecrawler;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;

/**
 * Holds information of an image's url, and it's target path on the local drive, 
 * where it should be downloaded.
 */
public class ImageItem {
	
	private HtmlAnchor url;	
	private String targetPath;
	
	public ImageItem(HtmlAnchor url, String targetPath) {
		this.url = url;
		this.targetPath = targetPath;
	}
	
	/**
	 * @return the url
	 */
	public HtmlAnchor getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(HtmlAnchor url) {
		this.url = url;
	}
	/**
	 * @return the targetPath
	 */
	public String getTargetPath() {
		return targetPath;
	}
	/**
	 * @param targetPath the targetPath to set
	 */
	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}	
}
