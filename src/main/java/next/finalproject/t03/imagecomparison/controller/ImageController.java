package next.finalproject.t03.imagecomparison.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;
import next.finalproject.t03.imagecomparison.dto.MostSimilarImageResponse;
import next.finalproject.t03.imagecomparison.service.ImageService;

@RestController
@CrossOrigin(origins = "*") 
@RequestMapping("/image")
public class ImageController {

	@Autowired
	private ImageService service;
	// consertando o erro de cors que a aplicação estava dando
	// pelo que eu entendi o cors regular quem acessa o backend
  
	// inserir imagem no banco de dados
	@PostMapping
	public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) throws IOException {
		if (service.isValidImageFile(file)) {
			String uploadImage = service.uploadImage(file);
			return ResponseEntity.status(HttpStatus.OK).body(uploadImage);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tipo de imagem invalida! ");
		}

	}

	// buscar imagem existente no banco de dados
	@GetMapping("/{fileName}")
	public ResponseEntity<?> downloadImage(@PathVariable String fileName) {
		byte[] imageData = service.downloadImages(fileName);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.valueOf("image/jpeg")).body(imageData);
	}

	// deletar imagem do banco de dados
	@DeleteMapping("/{longId}")
	public ResponseEntity<?> deleteImage(@PathVariable Long longId) {
		if (service.deleteImage(longId)) {
			return ResponseEntity.status(HttpStatus.OK).body("Imagems removida com sucesso! ");
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Imagem não está no banco de dados! ");
	}

	// inserir imagem para comparar com as imagens salvas no banco de dados
	@PostMapping("/getMostSimilar")
	public ResponseEntity<?> getMostSimilarImage(@RequestParam("image") MultipartFile file) {

		if (service.isValidImageFile(file)) {

			try {
				MostSimilarImageResponse imageData = service.getMostSimilarImage(file);

				if (imageData.getImage() != null) {

					return ResponseEntity.status(HttpStatus.OK)
							.contentType(MediaType.valueOf("image/jpeg"))
							.body(imageData.getImage());

				} else {
					return ResponseEntity.status(HttpStatus.OK).body(imageData.getResponseMessage());
				}

			} catch (IOException ex) {

				System.out.println("Erro de entrada e saida nos arquivos");
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

			}

		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tipo de imagem invalida! ");
		}

	}

	// inserir duas imagem para comparar sem salvar no banco de dados
	@PostMapping("/compareTwoImages")
	public ResponseEntity<?> getSimilarity(@RequestParam("image1") MultipartFile file1,
			@RequestParam("image2") MultipartFile file2) throws IOException {
		
		Map<String, String> data = new HashMap<>();

		if (service.isValidImageFile(file1) && service.isValidImageFile(file2)) {

			HashingAlgorithm hasher = new PerceptiveHash(32);
			// classe formata o valor de similaridade
			java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");

			File firsImage = service.convertMultiPartToFile(file1);
			File secondImage = service.convertMultiPartToFile(file2);

			Hash hash0 = hasher.hash(firsImage);
			Hash hash1 = hasher.hash(secondImage);

			double similarityScore = hash0.normalizedHammingDistance(hash1);

			if (similarityScore == 0) {
				data.put("score", "São iguais! " + "Score de similaridade = " + df.format(similarityScore));
				return new ResponseEntity<>(data, HttpStatus.OK);
			} else if (similarityScore < .4) {
				data.put("score", "São similares! " + "Score de similaridade = " + df.format(similarityScore));
				return new ResponseEntity<>(data, HttpStatus.OK);

			} else {
				data.put("score", "São diferentes! " + "Score de similaridade = " + df.format(similarityScore));
				return new ResponseEntity<>(data, HttpStatus.OK);

			}

		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tipo de imagem invalida! ");
		}
	}
}
