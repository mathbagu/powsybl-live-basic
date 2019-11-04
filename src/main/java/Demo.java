/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.commons.io.table.AsciiTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.ContingenciesProviderFactory;
import com.powsybl.contingency.ContingenciesProviders;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.*;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * @author Mathieu Bague <mathieu.bague@rte-france.com>
 */
public class Demo {

    private static final String EMPTY_LINES = "\n\n\n";

    public static void main(String[] args) throws IOException {
        // 1. Load Elia (BE) and Tennet (NL) micro grid files
        Network networkBE = Importers.loadNetwork(Demo.class.getResource("/data/be.zip").getPath());
        System.out.println("Network loaded: " + networkBE.getId() + " (" + networkBE.getSubstationCount() + ")");

        Network networkNL = Importers.loadNetwork(Demo.class.getResource("/data/nl.zip").getPath());
        System.out.println("Network loaded: " + networkNL.getId() + " (" + networkNL.getSubstationCount() + ")");

        // 2. Merge them together
        System.out.println(EMPTY_LINES);

        Network network = Network.create("merge", "CGMES");
        network.merge(networkBE, networkNL);
        System.out.println("Networks merged: " + network.getId() + " (" + network.getSubstationCount() + ")");

        // 3. Run a Load-flow and display the violations
        System.out.println(EMPTY_LINES);
        LoadFlowParameters lfParameters = new LoadFlowParameters();
        LoadFlowResult result = LoadFlow.run(network, lfParameters);
        System.out.println("LoadFlow: " + result.isOk() + " " + result.getMetrics());
        if (result.isOk()) {
            System.out.println("Violations:\n" + Security.printLimitsViolations(network, true));
        }

        // 4. Run a security analysis and display the violations
        System.out.println(EMPTY_LINES);
        ContingenciesProviderFactory ctgFactory = ContingenciesProviders.newDefaultFactory();
        ContingenciesProvider ctgProvider = ctgFactory.create(Demo.class.getResourceAsStream("/data/contingencies.groovy"));

        SecurityAnalysisFactory saFactory = SecurityAnalysisFactories.newDefaultFactory();
        SecurityAnalysis sa = saFactory.create(network, new LocalComputationManager(), 0);
        SecurityAnalysisParameters saParameters = new SecurityAnalysisParameters();

        SecurityAnalysisResult saResult = sa.run(VariantManagerConstants.INITIAL_VARIANT_ID, saParameters, ctgProvider).join();

        Security.print(saResult, network, new OutputStreamWriter(System.out), new AsciiTableFormatterFactory(), new TableFormatterConfig());
    }
}
