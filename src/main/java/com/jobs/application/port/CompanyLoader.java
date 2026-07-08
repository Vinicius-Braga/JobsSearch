package com.jobs.application.port;

import com.jobs.domain.Company;

import java.io.IOException;
import java.util.List;

public interface CompanyLoader {
    List<Company> load() throws IOException;
}
