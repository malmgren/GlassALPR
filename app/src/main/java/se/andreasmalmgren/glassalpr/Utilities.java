package se.andreasmalmgren.glassalpr;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by amaxp2 on 2015-03-12.
 */
public class Utilities {
    /*public static BufferedImage matToBufferedImage(Mat image) throws IOException {
        MatOfByte bytemat = new MatOfByte();
        Highgui.imencode(".jpg", image, bytemat);
        byte[] bytes = bytemat.toArray();
        InputStream in = new ByteArrayInputStream(bytes);
        BufferedImage bufferedImage = ImageIO.read(in);
        return bufferedImage;
    }

    public static Image matToBufferedImageArrayCopy(Mat m){
        int type = BufferedImage.TYPE_BYTE_GRAY;

        if ( m.channels() > 1){
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    public static void displayImage(Image img2){
        //BufferedImage img=ImageIO.read(new File("/HelloOpenCV/lena.png"));
        ImageIcon icon=new ImageIcon(img2);
        JFrame frame=new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(img2.getWidth(null)+50, img2.getHeight(null)+50);
        JLabel lbl=new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public static void displayMat(Mat image){
        Utilities.displayImage(Utilities.matToBufferedImageArrayCopy(image));
    }*/

    public static void writeMatToFile(String path, String filename, Mat image){
        Highgui.imwrite(path + filename, image);
    }

    public static List<String> getFilesFromPath(String path){
        final File folder = new File(path);
        List<String> fileList = listFilesForFolder(folder);

        return fileList;
    }

    public static List<String> listFilesForFolder(final File folder) {
        List<String> fileList = new ArrayList<String>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {

                fileList.add(fileEntry.getName());
            }
        }
        return fileList;
    }
}
