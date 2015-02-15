package nordlib.net.imagecrawler;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;


/**
 * Image crawler core class for 4chan (Yotsuba) type imageboards. 
 * @author Norder
 *
 */
public class ImageCrawler {

	public final WebClient webClient;
	public static List<String> boardTitles;
	public List<ImageItem> imageUrlRepo;
	public final static int THREAD_IMAGE_CAPACITY = 500;
	
	@SuppressWarnings("deprecation")
	public ImageCrawler() {
		 webClient = new WebClient();
		 webClient.setJavaScriptEnabled(false);
		 ImageCrawler.boardTitles = new ArrayList<String>();
		 imageUrlRepo = new ArrayList<ImageItem>();
	}
	
	/**
	 * Returns the links (HtmlAnchors) from the contents of a page specified by an url String
	 * @param url A String url
	 * @return Returns a List of HtmlAnchor objects
	 * @throws FailingHttpStatusCodeException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public List<String> getBoardLinksFromUrl(String url) throws FailingHttpStatusCodeException, MalformedURLException, IOException {				
		  String currentUrl = url;
		  String currentUrlWithDoubleSlash = currentUrl.replace("http://", "//");
		  HtmlPage page = webClient.getPage(currentUrl);  
		  List<HtmlAnchor> list = page.getAnchors();
		  for (int i=list.size()-1; i>=0; i--) {
			  String link = list.get(i).getHrefAttribute();

			  // Example for fileshares link: "//rs.4chan.org/"
			  // Example for frames link: "http://www.4chan.org/frames"
			  if (link.contains("#") || link.length() < 4 || link.matches("^\\/[a-z]*$") || link.contains("/rs.") || link.endsWith("/frames/") || !link.contains("//boards.") || link.equals(currentUrl) || link.equals(currentUrlWithDoubleSlash)) {
				  list.remove(i);
			  }  
		  }		  
		  
		  List<String> urlStringList = new ArrayList<String>();
		  for (HtmlAnchor anchor : list) {
			  urlStringList.add(anchor.getHrefAttribute());
			  // pattern: //boards.4chan.org/cgl/
			  
			  String mydata = anchor.getHrefAttribute();
			  Pattern pattern = Pattern.compile("/([a-z0-9]*?)/$");
			  Matcher matcher = pattern.matcher(mydata);
			  if (matcher.find())
			  {
			      //System.out.println("title: " + matcher.group(1));
			      ImageCrawler.boardTitles.add("/"+matcher.group(1)+"/");
			  }			    
		  }		  
		  return urlStringList;
	}
	
	
	/**
	 * Returns the HtmlAnchors of the images found on the HtmlPage specified in the parameter.
	 * @param page
	 * @return
	 */
	public List<HtmlAnchor> getImgAnchors(HtmlPage page) {
		List<HtmlAnchor> linksOnPage = page.getAnchors();
		for (int i=linksOnPage.size()-1; i>=0; i--) {
			  String link = linksOnPage.get(i).getHrefAttribute();
			  
			  if (!(link.endsWith(".jpg") || link.endsWith(".png") || link.endsWith(".gif"))) {
				  linksOnPage.remove(i);
			  }			  			  
		}	
		return linksOnPage;
	}
	
	/**
	 * Returns the HtmlAnchors of html pages found on the HtmlPage specified in the parameter.
	 * @param currentUrl
	 * @param page
	 * @return
	 */
	public List<String> getThreadAndPageLinks(String currentUrl, HtmlPage page) {
		// Getting the img anchors
		//TODO: REFACTOR
		List<HtmlAnchor> linksOnPage = page.getAnchors();
		List<String> linkStringsOnPage = new ArrayList<String>();
		
		for (HtmlAnchor anchor : linksOnPage) {
			if (!linkStringsOnPage.contains(anchor.getHrefAttribute())) {
				linkStringsOnPage.add(anchor.getHrefAttribute());				
			}
		}
		
		for (int i=linkStringsOnPage.size()-1; i>=0; i--) {
			  String link = linkStringsOnPage.get(i);
			  
			  if (!link.matches("^(?!res/).*[/0-9]$")) { 
				  linkStringsOnPage.remove(i);
			  } else if (link.contains("#")) {
				  linkStringsOnPage.remove(i);
			  } else if (ImageCrawler.boardTitles.contains(link)) {
				  linkStringsOnPage.remove(i);
			  }			  
		}	
		
		return linkStringsOnPage;
	}
	
	
//	/**
//	 * Downloads an image by an anchor
//	 * @param anchorAttachment
//	 * @throws IOException 
//	 */
//	public void downloadImage(HtmlElement anchorAttachment, String path) throws IOException {
//		
//		String fileName = anchorAttachment.getAttribute("href").substring(anchorAttachment.getAttribute("href").lastIndexOf("/"));
//		
//		try {
//			
//			InputStream is = anchorAttachment.click().getWebResponse().getContentAsStream();
//			OutputStream out = new FileOutputStream(new File(path+fileName));
//			
//			int read=0;
//			byte[] bytes = new byte[1024];
//			
//			while((read = is.read(bytes)) != -1) {
//				out.write(bytes, 0, read);
//			}
//			
//			is.close();
//			out.flush();
//			out.close();    
//			
//			System.out.println("New file created!");
//		}
//		catch (IOException e){
//			System.out.println(e.getMessage());
//		}
//		catch (FailingHttpStatusCodeException fe) {
//			System.out.println(fe.getMessage());			
//		}
//	}
	
	
	@SuppressWarnings("unused")
	public void downloadImages() {
				
		if (this.imageUrlRepo.size() <= THREAD_IMAGE_CAPACITY) {
			System.out.println("less than "+THREAD_IMAGE_CAPACITY+" elements found");
			

			ImageSaver imageSaver = new ImageSaver(0, this.imageUrlRepo);
			imageSaver.run();
				
			
		} else {
			System.out.println("more than "+THREAD_IMAGE_CAPACITY+" elements found");
			
			ArrayList<ArrayList<ImageItem>> imageUrlRepoList = new ArrayList<ArrayList<ImageItem>>();
			imageUrlRepoList.add(new ArrayList<ImageItem>());
			int s = this.imageUrlRepo.size();
			for (int i=0; i<this.imageUrlRepo.size()-1; i++) {
				if (i%THREAD_IMAGE_CAPACITY==0 && i>0) {
					imageUrlRepoList.add(new ArrayList<ImageItem>());
				}
			
				imageUrlRepoList.get(imageUrlRepoList.size()-1).add(
						new ImageItem(this.imageUrlRepo.get(i).getUrl(), this.imageUrlRepo.get(i).getTargetPath())
						);
				
			}
			
			System.out.println("Will execute " + imageUrlRepoList.size() + " threads to download.");
						
			for (int i=0; i<imageUrlRepoList.size(); i++) {		
				if (i!=0) {
					System.gc();
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				ImageSaver imageSaver = new ImageSaver(i, imageUrlRepoList.get(i));
				imageSaver.start();
				
				System.out.println("beggining waiting loop");
				while (imageSaver.alive) {
					try {
						System.out.print(".");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println("finishing waiting loop");
					
				imageSaver = null;		
			}
		}
	}
	
	
}
