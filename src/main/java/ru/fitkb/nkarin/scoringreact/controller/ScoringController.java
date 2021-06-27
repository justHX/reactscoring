package ru.fitkb.nkarin.scoringreact.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.fitkb.nkarin.scoringreact.model.PersonDataRq;
import ru.fitkb.nkarin.scoringreact.model.PersonDataRs;
import ru.fitkb.nkarin.scoringreact.service.ScoringService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ScoringController {

	private final ScoringService scoringService;

	@PostMapping("/scoring/scoringCalc")
	public Mono<PersonDataRs> scoringCalc(@RequestBody PersonDataRq personDataRq) {

		log.debug("Request personDataRq by data = {}", personDataRq);

		Mono<Integer> scoring = scoringService.processingPersonData(personDataRq);

		Mono<PersonDataRs> personDataRs = scoringService.checkScoring(scoring,
				personDataRq.getInn());

		scoringService.saveToDbPersonData(personDataRs, personDataRq);

		return personDataRs;
	}

	@GetMapping("/scoring/takePersonData/{serialNumber}")
	public Mono<ResponseEntity> getLiteCustomerById(@PathVariable String serialNumber) {
		log.debug("Requested person data by serialNumber = {}", serialNumber);

		return scoringService.getPersonData(serialNumber).map(ResponseEntity::ok);

	}
}
