package nordlib.net.imagecrawler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 *	Image crawler for 4chan (Yotsuba) type imageboards. 
 *	@author Norder
 *  TODO: Refactor the whole thing! Move more code into the ImageCrawler class.
 *
 */
public class ImageCrawlerRunner {
	
	private static final String START_URL = "http://www.4chan.org/";
	private static final String IMAGE_REPO = "d:\\ImageCrawlerRepo\\";	//TODO: make this as a program parameter
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		
		// Initialization
		ImageCrawler imageCrawler = new ImageCrawler();
		final WebClient webClient = imageCrawler.webClient;
		webClient.setPrintContentOnFailingStatusCode(false);		
		List<String> linkList = imageCrawler.getBoardLinksFromUrl(START_URL);
		List<String> visitedUrlList = new ArrayList<String>();
		String selectedBoardURL = "//boards.4chan.org/mlp/";
		
		
		
		if (args.length == 0) {
	
			System.out.println("Select a board to download. Use one of the followings as parameter:");
			for (int i=linkList.size()-1; i>0; i--) {
				//TODO: make the selection easier?, by presenting only the board names like /b/ or /mlp/ as a choice. 
				System.out.println("\t"+linkList.get(i));
				if (!linkList.get(i).equals(selectedBoardURL)) {
					linkList.remove(i);
				}
			}
			
		} else {
		
			System.out.println("arg: " + args[0]);
			if (!linkList.contains(args[0])) {
				System.err.println("Wrong parameter! Did not found such a board url.");
				return;
			}
			selectedBoardURL = args[0];
			
			// Creating a directory for the image repository
			Files.createDirectories(Paths.get(IMAGE_REPO));
		
			// Processing the list of links
			while( linkList.size() > 0 ) {
					
				System.out.print("["+((visitedUrlList.size()==0)?0:(int)((float)visitedUrlList.size()/(float)(linkList.size()+visitedUrlList.size())*100.0))+"%] [Total page urls found: "+(linkList.size()+visitedUrlList.size())+"] - ");
				
				// Getting the first element from the list..
				String currentUrl = linkList.get(0);
				if (!currentUrl.startsWith("http://") && currentUrl.startsWith("//")) {
					currentUrl = "http:" + currentUrl;
				}
				System.out.println("Opening URL: " + currentUrl);
				
				//TODO: watch for Exception in thread "main" java.net.MalformedURLException
				HtmlPage page;
				try {
					page = webClient.getPage(currentUrl);
				} catch (FailingHttpStatusCodeException e) {
					System.out.println("404 Error: " + currentUrl);
					if (!visitedUrlList.contains(linkList.get(0))) {
						visitedUrlList.add(linkList.get(0));
					}			
					linkList.remove(0);
					continue;
				}
				
				// Board title:
				// Pattern: <div class="boardTitle">/k/ - Weapons</div>
				HtmlElement boardTitleHtmlElement = (HtmlElement) page.getByXPath("//div[@class='boardTitle']").get(0);
				String boardTitle = boardTitleHtmlElement.getTextContent();
				
				if (!currentUrl.contains(selectedBoardURL)) {
					if (!visitedUrlList.contains(linkList.get(0))) {
						visitedUrlList.add(linkList.get(0));
					}			
					linkList.remove(0);
					continue;				
				}
				
				// Getting the img anchors
				List<HtmlAnchor> imageLinksOnPage = imageCrawler.getImgAnchors(page);
				
				// Creating a folder
				// TODO: creating all these folders are not necessary
				String boardPath = IMAGE_REPO+boardTitle.replace("/", "");
				if (!Files.exists(Paths.get(boardPath))) {
					Files.createDirectories(Paths.get(boardPath));			
				}
				
				// Downloading an image by an anchor
				for (int k=0; k<imageLinksOnPage.size(); k++) {
					//System.out.println("Should download: " + linksOnPage.get(k).getAttribute("href"));
					//imageCrawler.downloadImage(linksOnPage.get(k), boardPath);
					boolean found = false;
					for (int i=0; i<imageCrawler.imageUrlRepo.size(); i++) {
						String url = imageCrawler.imageUrlRepo.get(i).getUrl().getAttribute("href");
						String newUrl = imageLinksOnPage.get(k).getAttribute("href");
						if (url.equals(newUrl)) {	
							found = true;
						} 
					}
					if (!found) {
						imageCrawler.imageUrlRepo.add(new ImageItem(imageLinksOnPage.get(k), boardPath));
					}
				}
				
				// Get only the page's links
				List<String> newList = imageCrawler.getThreadAndPageLinks(currentUrl, page);
				List<String> newListWithCurrentUrl = new ArrayList<String>();
				
				for (int i=newList.size()-1; i>=0; i--) {
					String link = newList.get(i);
					
					// href="thread/2511717/orange-challenge">Reply</a>
					// <a href="10">10</a>
					if (!(link.matches("^[0-9]*$") ||  link.startsWith("thread/"))) {
						newList.remove(i);			
					}  else {		
						if (!currentUrl.contains("thread") && currentUrl.matches("^.+?\\d$") && link.matches("^[0-9]*$")) {
							newListWithCurrentUrl.add(0, currentUrl.substring(
								0, (currentUrl.lastIndexOf("/")))  + "/" + newList.get(i) 
							);			
						} else {
							// http://boards.4chan.org/vr/6/thread/2219995      - link: thread/2220510
							if (currentUrl.contains("thread") && link.startsWith("thread/")) {
								newListWithCurrentUrl.add(0, currentUrl.substring(0, currentUrl.lastIndexOf("/thread/")) + "/" + link);
							} else if (currentUrl.contains("thread") && currentUrl.matches("^.+?\\d$") && link.matches("^[0-9]*$")) {
								// currentUrl:
								// http://boards.4chan.org/mlp/2/thread/21717688
								// link:
								// 3
								
								// http://boards.4chan.org/mlp/2/thread/21729978
								//TODO: put all this kind of regex magic to a method
								Matcher m = Pattern.compile("(.*)/[0-9]*/thread/(.*)").matcher(currentUrl);
								String endOfUrl = null;
								if(m.matches())
								{
									endOfUrl = m.group(1);
								}
								if (endOfUrl == null) {
									System.out.println("Bad regex pattern - Fix me!: " + currentUrl + " " + link);
									throw new Exception("Bad regex pattern - Fix me!: " + currentUrl + " " + link);
								}
								newListWithCurrentUrl.add(0, endOfUrl + "/" + link);

							} else {
								if (currentUrl.endsWith("/")) {
									newListWithCurrentUrl.add(0, currentUrl+newList.get(i));
								} else {
									newListWithCurrentUrl.add(0, currentUrl+"/"+newList.get(i));								
								}						
							}							
						}						
					}		
				}	  
					
				// Merge the list with newList
				for (String nextNewUrl : newListWithCurrentUrl) {
					if (!linkList.contains(nextNewUrl) && !visitedUrlList.contains(nextNewUrl)) {
						linkList.add(nextNewUrl);
					}
				}
				
				if (!visitedUrlList.contains(linkList.get(0))) {
					visitedUrlList.add(linkList.get(0));
				}			
				linkList.remove(0);
			}
		
			webClient.closeAllWindows();
			
			System.out.println("Total images found: "+imageCrawler.imageUrlRepo.size());
			imageCrawler.downloadImages();
			
		}
	}

}
