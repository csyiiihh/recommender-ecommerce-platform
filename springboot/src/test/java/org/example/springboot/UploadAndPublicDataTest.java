package org.example.springboot;
import org.example.springboot.entity.*;
import org.example.springboot.util.FileUtil;
import org.example.springboot.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class UploadAndPublicDataTest {
    @Test void publicMerchantSerializationDoesNotExposeEmailOrLicense() throws Exception {
        User u=new User();u.setId(1L);u.setName("书店");u.setEmail("private@example.com");u.setBusinessLicense("/img/private.png");u.setPassword("secret");
        Product p=new Product();p.setMerchant(u);
        var json=new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(p)).get("merchant");
        assertEquals(1L,json.get("id").asLong());assertEquals("书店",json.get("name").asText());assertEquals(2,json.size());
    }
    @Test void htmlAndSvgCannotBeUploadedAsImages() {
        assertThrows(BusinessException.class,()->FileUtil.saveImage(new MockMultipartFile("file","x.svg","image/svg+xml","<svg/>".getBytes()),null));
        assertThrows(BusinessException.class,()->FileUtil.saveImage(new MockMultipartFile("file","x.png","image/png","<html>bad</html>".getBytes()),null));
    }
    @Test void validImagesAreReencodedWithUniqueNames() throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String folder="test-"+java.util.UUID.randomUUID();String first=null,second=null;
        try {
            var input=new MockMultipartFile("file","cover.png","image/png",bytes.toByteArray());
            first=FileUtil.saveImage(input,folder);second=FileUtil.saveImage(input,folder);
            assertNotEquals(first,second);assertTrue(first.endsWith(".png"));assertFalse(first.contains("cover"));
            assertNotNull(ImageIO.read(Path.of(FileUtil.FILE_BASE_PATH,first.substring(1)).toFile()));
        } finally {
            if(first!=null)FileUtil.deleteFile(first);if(second!=null)FileUtil.deleteFile(second);
            Files.deleteIfExists(Path.of(FileUtil.FILE_BASE_PATH,"img",folder));
        }
    }
    @Test void imageDirectoryCannotEscapeStorageRoot() throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"png",bytes);
        assertThrows(BusinessException.class,()->FileUtil.saveImage(new MockMultipartFile("file","ok.png","image/png",bytes.toByteArray()),"../../../outside"));
    }
}
