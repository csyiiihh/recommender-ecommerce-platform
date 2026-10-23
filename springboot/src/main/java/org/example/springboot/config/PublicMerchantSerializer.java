package org.example.springboot.config;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.example.springboot.entity.User;
import org.example.springboot.dto.MerchantSummary;
import java.io.IOException;
public class PublicMerchantSerializer extends JsonSerializer<User> {
    @Override public void serialize(User user, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeObject(new MerchantSummary(user.getId(), user.getName()));
    }
}
