package io.lawliet.contactapi.service;

import io.lawliet.contactapi.domain.Contact;
import io.lawliet.contactapi.repo.ContactRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.internal.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;

import static io.lawliet.contactapi.constant.Constant.PHOTO_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service // Make this class as a spring service
@Slf4j // Create a logger
@Transactional(rollbackOn = Exception.class) // Rolled back all data when exception is thrown
@RequiredArgsConstructor // If this class is called, it requires contactRepo as a parameter (auto create constructor)
public class ContactService {
    private final ContactRepo contactRepo;

    public Page<Contact> getAllContacts(int page, int size) {
        return contactRepo.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    public Contact getContact(String id) {
        return contactRepo.findById(id).orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    public Contact createContact(Contact contact) {
        return contactRepo.save(contact);
    }

    public void deleteContact(String id) {
        Contact contact = getContact(id);
        if (contact != null) {
            contactRepo.delete(contact);
        }
    }

    public String uploadPhoto(String id, MultipartFile file) {
        log.info("Saving picture for user ID: {}", id);
        Contact contact = getContact(id);
        String photoUrl = photoFunction.apply(id, file);
        contact.setPhotoUrl(photoUrl);
        contactRepo.save(contact);
        return photoUrl;
    }

    private final Function<String, String> fileExtension = filename -> Optional.of(filename) // Handle cases where the filename might not contain an extension
                    .filter(name -> name.contains(".")) // Check filename must contain "."
                    .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)) // Extract file extension. It begins at index of "." + 1 (first alphabet after ".") into end
                    .orElse(".png"); // If filename not contain ".", it defaults as ".png"

    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = id + fileExtension.apply(image.getOriginalFilename());
        try {
            // Get current directory of file
            Path fileStorageLocation = Paths.get(PHOTO_DIRECTORY).toAbsolutePath().normalize();
            if (!Files.exists(fileStorageLocation)) {
                // If the directory doesn't exist, create it
                Files.createDirectories(fileStorageLocation);
            }

            // Copy input stream to a file. If the file already exist, replace the file
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);

            // Create URL with path "/contacts/image/" + filename
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/contacts/image/" + filename)
                    .toUriString();

        } catch (Exception e) {
            throw new RuntimeException("Unable to save image");
        }
    };
}



