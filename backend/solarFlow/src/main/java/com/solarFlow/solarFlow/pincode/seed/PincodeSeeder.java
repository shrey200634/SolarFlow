package com.solarFlow.solarFlow.pincode.seed;

import com.solarFlow.solarFlow.pincode.domain.ServiceablePincode;
import com.solarFlow.solarFlow.pincode.repo.ServiceablePincodeRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class PincodeSeeder implements ApplicationRunner {

    private final ServiceablePincodeRepo pincodes;

    @Override
    public void run(ApplicationArguments args) {

        if (pincodes.count() > 0) {
            return;
        }

        List<String> sample = List.of(
                "440001", "440008", "440010", "440012", "440013",
                "440015", "440016", "440022", "440024", "440025");

        for (String code : sample) {
            pincodes.save(ServiceablePincode.builder()
                    .pincode(code)
                    .areaName("Nagpur (sample)")
                    .active(true)
                    .build());
        }

        log.info("Seeded {} sample serviceable pincodes.", sample.size());
    }
}