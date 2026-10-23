package org.example.springboot.config;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import java.io.IOException;
public class ImageResourceResolver extends PathResourceResolver {
    @Override protected Resource getResource(String resourcePath, Resource location) throws IOException {
        if (!resourcePath.matches("(?i).+\\.(png|jpe?g|gif|webp|avif|ico)")) return null;
        return super.getResource(resourcePath, location);
    }
}
