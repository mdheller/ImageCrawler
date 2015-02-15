package nordlib.net.imagecrawler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlElement;

/**
 * A thread that downloads batches of images.
 * @author Norder
 *
 */
public class ImageSaver extends Thread {
	
	private int id;
	private List<ImageItem> imageUrlRepo;
	public boolean alive;
	
	public ImageSaver(int id, List<ImageItem> ic) {
		this.id = id;
		this.imageUrlRepo = ic;
		this.alive = true;
	}
	
	
	@Override
	public void run() {
		System.out.println("Starting thread #"+id);
		try {
			for (int i=0; i<imageUrlRepo.size(); i++) {
				
				int size = imageUrlRepo.size();
				System.out.println("["+ (i+1) + "/" + size +"] Downloading: " + imageUrlRepo.get(i).getUrl().getAttribute("href"));
				
				HtmlElement anchorAttachment = imageUrlRepo.get(i).getUrl(); 
				String path = imageUrlRepo.get(i).getTargetPath();
				String fileName = anchorAttachment.getAttribute("href").substring(anchorAttachment.getAttribute("href").lastIndexOf("/"));
				
				InputStream inputStream  = anchorAttachment.click().getWebResponse().getContentAsStream();				
				FileOutputStream out = new FileOutputStream(new File(path+fileName));
				  
				try
				{					
				    byte[] buf=new byte[8192];
				    int bytesread = 0, bytesBuffered = 0;
				    while( (bytesread = inputStream.read( buf )) > -1 ) {
				        out.write( buf, 0, bytesread );
				        bytesBuffered += bytesread;
				        if (bytesBuffered > 1024 * 1024) { //flush after 1MB
				            bytesBuffered = 0;
				            out.flush();
				        }
				    }
				}
				finally {
					if (inputStream != null) {
						inputStream.close();
					}
				    if (out != null) {
				        out.flush();
				        out.close();
				    }
				}		
			}
		}
		catch (IOException e){
			System.out.println(e.getMessage());
		}
		catch (FailingHttpStatusCodeException fe) {
			System.out.println(fe.getMessage());			
		} finally {
			this.alive = false;
		}
	}

	public void finalize() {
		this.alive = false;
		System.out.println("Closing thread #"+id);
	}
	
}
