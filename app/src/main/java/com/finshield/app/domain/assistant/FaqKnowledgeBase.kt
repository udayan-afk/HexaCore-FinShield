package com.finshield.app.domain.assistant

/**
 * Curated local FAQ Knowledge Base for FinShield Assistant.
 * Provides clear, educational answers suitable for beginners across 5 key financial categories.
 */
object FaqKnowledgeBase {

    val items: List<FaqItem> = listOf(
        // ==========================================
        // CATEGORY 1: LOANS
        // ==========================================
        FaqItem(
            id = "loan_personal",
            category = FaqCategory.LOANS,
            question = "What is a personal loan?",
            answer = "A personal loan is an unsecured installment loan provided by banks and NBFCs for personal use, such as medical emergencies, home renovation, or debt consolidation. Unlike a home or auto loan, you do not need to pledge collateral, and repayment is made via fixed monthly EMIs over an agreed tenure.",
            keywords = listOf("personal loan", "unsecured", "collateral", "borrowing", "installment"),
            relatedQuestionIds = listOf("loan_secured_unsecured", "loan_process", "emi_definition"),
        ),
        FaqItem(
            id = "loan_secured_unsecured",
            category = FaqCategory.LOANS,
            question = "What is a secured versus unsecured loan?",
            answer = "A secured loan requires collateral or an asset (such as gold, property, or a vehicle) that the lender can claim if you fail to repay. Because lender risk is lower, secured loans usually carry lower interest rates. An unsecured loan (like a personal loan or credit card) requires no asset pledge, but interest rates are generally higher and approval depends heavily on your credit score and income.",
            keywords = listOf("secured", "unsecured", "collateral", "difference", "asset", "pledge", "risk"),
            relatedQuestionIds = listOf("loan_personal", "cibil_factors", "interest_reducing"),
        ),
        FaqItem(
            id = "loan_process",
            category = FaqCategory.LOANS,
            question = "How does the loan application process work?",
            answer = "The standard loan application involves 5 key steps:\n1. Eligibility check & application submission.\n2. Document submission (KYC, income proof, bank statements).\n3. Credit bureau assessment (CIBIL check).\n4. Underwriting & issuance of a loan sanction letter.\n5. Agreement signing, NACH/e-mandate setup, and net loan disbursal into your bank account.",
            keywords = listOf("application", "process", "steps", "how to apply", "workflow", "sanction"),
            relatedQuestionIds = listOf("loan_documents", "safety_sanction_letter", "safety_net_disbursal"),
        ),
        FaqItem(
            id = "loan_documents",
            category = FaqCategory.LOANS,
            question = "What documents are usually required?",
            answer = "Lenders generally require:\n• Identity & Address Proof: Aadhaar, PAN card, Passport, or Voter ID.\n• Income Proof for Salaried: Recent salary slips (last 3 months) and Form 16.\n• Income Proof for Self-Employed: Income Tax Returns (ITR for 2-3 years) with balance sheets.\n• Banking Proof: Last 3–6 months bank statements showing regular salary or business cash flow.",
            keywords = listOf("documents", "required", "paperwork", "pan", "aadhaar", "salary slips", "bank statement", "itr"),
            relatedQuestionIds = listOf("safety_kyc", "safety_bank_statement", "loan_process"),
        ),
        FaqItem(
            id = "loan_tenure",
            category = FaqCategory.LOANS,
            question = "What is loan tenure?",
            answer = "Loan tenure is the total duration agreed upon between you and the lender to repay the borrowed amount along with interest. Personal loan tenures typically range from 12 to 60 months. A longer tenure lowers your monthly EMI but increases the total interest paid over the life of the loan. A shorter tenure increases the monthly EMI but saves you significant interest costs.",
            keywords = listOf("tenure", "duration", "term", "months", "years", "time period"),
            relatedQuestionIds = listOf("emi_definition", "emi_calculation", "loan_foreclosure"),
        ),
        FaqItem(
            id = "loan_foreclosure",
            category = FaqCategory.LOANS,
            question = "What is foreclosure or prepayment?",
            answer = "Prepayment means paying a lump-sum amount toward your loan before the scheduled EMI date, which reduces your outstanding principal. Foreclosure means paying off the entire remaining loan balance in a single payment to close the account early. Under RBI guidelines, floating-rate personal and home loans to individual borrowers cannot attract foreclosure or prepayment penalties.",
            keywords = listOf("foreclosure", "prepayment", "pre-closing", "early payment", "penalty", "close early"),
            relatedQuestionIds = listOf("loan_tenure", "interest_reducing", "finshield_analyzer"),
        ),
        FaqItem(
            id = "loan_missed_emi",
            category = FaqCategory.LOANS,
            question = "What happens when an EMI is missed?",
            answer = "Missing an EMI causes several consequences:\n1. Late payment penalty & penal interest charges from the lender.\n2. NACH/e-mandate bounce charges from both the lender and your bank.\n3. Negative reporting to credit bureaus (CIBIL, Experian, Equifax), which drops your credit score.\n4. Difficulty obtaining future loans or credit cards. If you anticipate repayment trouble, contact your lender before the due date to request restructuring.",
            keywords = listOf("missed emi", "bounce", "late payment", "default", "delay", "consequences", "penalty"),
            relatedQuestionIds = listOf("cibil_factors", "cibil_maintain", "emi_definition"),
        ),

        // ==========================================
        // CATEGORY 2: EMI AND INTEREST
        // ==========================================
        FaqItem(
            id = "emi_definition",
            category = FaqCategory.EMI_AND_INTEREST,
            question = "What is EMI?",
            answer = "EMI stands for Equated Monthly Instalment. It is a fixed monetary amount paid by a borrower to a lender on a specific date each month. Each EMI is composed of two components: principal repayment (the actual borrowed money) and interest on the borrowed amount.",
            keywords = listOf("what is emi", "equated monthly installment", "definition", "monthly payment"),
            relatedQuestionIds = listOf("emi_calculation", "interest_reducing", "interest_flat"),
        ),
        FaqItem(
            id = "emi_calculation",
            category = FaqCategory.EMI_AND_INTEREST,
            question = "How is EMI calculated?",
            answer = "Standard loans use the reducing-balance EMI formula:\nEMI = P × r × (1 + r)^n / ((1 + r)^n − 1)\nwhere:\n• P = Principal loan amount\n• r = Monthly interest rate (Annual rate ÷ 12 ÷ 100)\n• n = Number of monthly instalments (tenure in months)\nYou can use the FinShield Loan Calculator on the home screen to test any amount and tenure.",
            keywords = listOf("how is emi calculated", "formula", "emi formula", "math", "computation"),
            relatedQuestionIds = listOf("emi_definition", "interest_reducing", "interest_flat"),
        ),
        FaqItem(
            id = "interest_reducing",
            category = FaqCategory.EMI_AND_INTEREST,
            question = "What is reducing-balance interest?",
            answer = "In a reducing-balance loan, interest is calculated every month only on the outstanding principal balance rather than the original loan amount. As you pay each EMI, the principal decreases, so the monthly interest portion decreases while the principal repayment portion increases. This is the borrower-friendly standard mandated by RBI for banks.",
            keywords = listOf("reducing balance", "diminishing balance", "reducing rate", "outstanding principal"),
            relatedQuestionIds = listOf("interest_flat", "interest_rate_vs_apr", "compare_loan_offers"),
        ),
        FaqItem(
            id = "interest_flat",
            category = FaqCategory.EMI_AND_INTEREST,
            question = "What is flat interest?",
            answer = "In a flat interest rate loan, interest is calculated on the entire original principal for the full tenure, regardless of how much principal you have already repaid. A flat rate of 10% can actually equal an effective reducing-balance rate of roughly 18% to 19%! FinShield's Agreement Analyzer automatically flags flat interest terms to protect you from hidden costs.",
            keywords = listOf("flat rate", "flat interest", "flat vs reducing", "effective rate", "misleading rate"),
            relatedQuestionIds = listOf("interest_reducing", "interest_rate_vs_apr", "finshield_analyzer"),
        ),
        FaqItem(
            id = "interest_rate_vs_apr",
            category = FaqCategory.EMI_AND_INTEREST,
            question = "What is the difference between interest rate and APR?",
            answer = "The nominal interest rate is solely the percentage cost of borrowing the principal. APR (Annual Percentage Rate) reflects the true total annual cost of borrowing because it includes the nominal interest rate plus upfront processing fees, verification charges, documentation costs, and mandatory insurance. Always compare APR rather than just the stated interest rate.",
            keywords = listOf("apr", "annual percentage rate", "difference", "total cost", "fees included"),
            relatedQuestionIds = listOf("fees_processing_gst", "compare_loan_offers", "finshield_analyzer"),
        ),
        FaqItem(
            id = "fees_processing_gst",
            category = FaqCategory.EMI_AND_INTEREST,
            question = "What are processing fees and GST?",
            answer = "A processing fee is a one-time fee charged by the lender to cover the administrative cost of processing and underwriting your loan application (typically 1% to 3% of the loan amount). In India, an 18% GST is levied on the processing fee. For example, on a ₹10,000 processing fee, you will pay ₹1,800 in GST, making the total fee ₹11,800. This is usually deducted upfront from your disbursal amount.",
            keywords = listOf("processing fee", "gst", "service charges", "upfront fee", "taxes", "18%"),
            relatedQuestionIds = listOf("safety_net_disbursal", "interest_rate_vs_apr", "compare_loan_offers"),
        ),
        FaqItem(
            id = "compare_loan_offers",
            category = FaqCategory.EMI_AND_INTEREST,
            question = "How do I compare two loan offers?",
            answer = "Do not rely on stated interest rate alone. To accurately compare offers:\n1. Compare the total repayment amount (all EMIs + all upfront fees).\n2. Verify whether each rate is reducing-balance or flat.\n3. Check processing fees, documentation charges, and GST.\n4. Review foreclosure and prepayment rules.\nYou can use FinShield's built-in 'Compare Loans' tab on the bottom bar to compare two offers side-by-side.",
            keywords = listOf("compare loans", "compare offers", "which loan is better", "evaluate", "two loans"),
            relatedQuestionIds = listOf("interest_rate_vs_apr", "fees_processing_gst", "finshield_analyzer"),
        ),

        // ==========================================
        // CATEGORY 3: CIBIL AND CREDIT HEALTH
        // ==========================================
        FaqItem(
            id = "cibil_score_definition",
            category = FaqCategory.CIBIL_AND_CREDIT,
            question = "What is a CIBIL score?",
            answer = "A CIBIL score is a 3-digit numerical summary of your credit history and repayment behavior, issued by TransUnion CIBIL. It ranges from 300 to 900. Lenders check this score to evaluate how reliably you repay loans and credit cards. A score of 750 or higher is generally considered excellent by Indian banks and qualifies you for the lowest interest rates.",
            keywords = listOf("cibil score", "credit score", "what is cibil", "transunion", "range 300 900"),
            relatedQuestionIds = listOf("cibil_factors", "cibil_maintain", "cibil_self_check"),
        ),
        FaqItem(
            id = "cibil_factors",
            category = FaqCategory.CIBIL_AND_CREDIT,
            question = "What factors affect a credit score?",
            answer = "Your credit score is calculated using 5 major factors:\n1. Payment History (~35%): Paying EMIs and credit card bills on time.\n2. Credit Utilization Ratio (~30%): Keeping credit card balances under 30% of total limits.\n3. Credit History Length (~15%): The age of your oldest active credit accounts.\n4. Credit Mix (~10%): A healthy balance of secured (home/auto) and unsecured (personal/card) credit.\n5. Recent Credit Inquiries (~10%): Avoiding multiple simultaneous loan applications.",
            keywords = listOf("factors", "affects score", "affects cibil", "cibil factors", "credit score factors", "utilization", "payment history", "algorithm", "calculation"),
            relatedQuestionIds = listOf("cibil_score_definition", "cibil_maintain", "cibil_self_check"),
        ),
        FaqItem(
            id = "cibil_maintain",
            category = FaqCategory.CIBIL_AND_CREDIT,
            question = "How can I maintain a good score?",
            answer = "To build and maintain a 750+ score:\n• Never miss an EMI or credit card due date; set up auto-debit.\n• Keep your credit card usage below 30% of your credit limit.\n• Maintain older credit cards to preserve your credit history length.\n• Avoid applying for multiple loans or cards in a short timeframe.\n• Check your credit report periodically to spot clerical errors or unauthorized accounts early.",
            keywords = listOf("maintain score", "improve score", "increase cibil", "good score", "750 plus", "tips"),
            relatedQuestionIds = listOf("cibil_factors", "cibil_correct_errors", "loan_missed_emi"),
        ),
        FaqItem(
            id = "cibil_self_check",
            category = FaqCategory.CIBIL_AND_CREDIT,
            question = "Does checking my own score affect it?",
            answer = "No. When you check your own credit score, it is classified as a 'soft inquiry'. Soft inquiries do not impact or lower your credit score in any way. Only 'hard inquiries'—which occur when a bank or lender checks your bureau report after you submit a loan or credit card application—can slightly lower your score if done frequently.",
            keywords = listOf("checking own score", "soft inquiry", "hard inquiry", "impact score", "does checking hurt"),
            relatedQuestionIds = listOf("cibil_score_definition", "cibil_factors", "cibil_maintain"),
        ),
        FaqItem(
            id = "cibil_rejection_reasons",
            category = FaqCategory.CIBIL_AND_CREDIT,
            question = "Why might a loan application be rejected?",
            answer = "Common reasons for loan rejection include:\n• Credit score below lender cutoff (typically under 650–700).\n• High FOIR (Fixed Obligation to Income Ratio): existing EMIs already consume >50% of your net income.\n• History of recent defaults, settlements, or written-off accounts.\n• Incomplete KYC or unverified employment/income proof.\n• Too many hard inquiries within the last 90 days.",
            keywords = listOf("rejected", "rejection", "declined", "why rejected", "reasons", "denied loan"),
            relatedQuestionIds = listOf("cibil_factors", "cibil_maintain", "finshield_eligibility"),
        ),
        FaqItem(
            id = "cibil_correct_errors",
            category = FaqCategory.CIBIL_AND_CREDIT,
            question = "How can I correct an error in my credit report?",
            answer = "If you notice an incorrect loan account, wrong outstanding balance, or false overdue status:\n1. Note the specific Account Number and lender reporting the error.\n2. Visit the official TransUnion CIBIL dispute portal (cibil.com/dispute-resolution).\n3. Submit an online dispute with supporting documents (e.g. NOC, payment receipt, bank statement).\n4. CIBIL will verify the dispute with the reporting lender. Under RBI rules, disputes must be investigated within 30 days.",
            keywords = listOf("dispute", "correct error", "wrong entry", "mistake in cibil", "dispute portal", "noc"),
            relatedQuestionIds = listOf("cibil_score_definition", "cibil_maintain", "safety_verify_lender"),
        ),

        // ==========================================
        // CATEGORY 4: BANKING AND FINANCIAL SAFETY
        // ==========================================
        FaqItem(
            id = "safety_kyc",
            category = FaqCategory.BANKING_SAFETY,
            question = "What is KYC?",
            answer = "KYC stands for 'Know Your Customer'. It is a mandatory identification process regulated by the Reserve Bank of India (RBI) to verify the customer's identity and address before opening accounts or disbursing loans. It helps prevent money laundering, financial fraud, and identity theft.",
            keywords = listOf("kyc", "know your customer", "verification", "aadhaar", "pan", "rbi mandate"),
            relatedQuestionIds = listOf("loan_documents", "safety_verify_lender", "safety_protect_credentials"),
        ),
        FaqItem(
            id = "safety_bank_statement",
            category = FaqCategory.BANKING_SAFETY,
            question = "What is a bank statement?",
            answer = "A bank statement is an official financial summary generated by your bank detailing all debit and credit transactions, account balances, and salary deposits over a specific period. Lenders examine statements to verify your steady income, active obligations, and cash flow health before sanctioning a loan.",
            keywords = listOf("bank statement", "transaction history", "account summary", "salary credit"),
            relatedQuestionIds = listOf("loan_documents", "safety_kyc", "finshield_aa_vs_cibil"),
        ),
        FaqItem(
            id = "safety_sanction_letter",
            category = FaqCategory.BANKING_SAFETY,
            question = "What is a loan sanction letter?",
            answer = "A loan sanction letter is a formal preliminary document issued by a lender approving your loan application in principle. It states the approved loan amount, annual interest rate, tenure, estimated EMI, processing fees, validity period, and conditions you must fulfill before the final loan agreement is executed and funds are disbursed.",
            keywords = listOf("sanction letter", "approval letter", "in principle", "offer letter", "conditions"),
            relatedQuestionIds = listOf("loan_process", "safety_net_disbursal", "finshield_analyzer"),
        ),
        FaqItem(
            id = "safety_net_disbursal",
            category = FaqCategory.BANKING_SAFETY,
            question = "What is net disbursal?",
            answer = "Net disbursal is the actual amount credited to your bank account after the lender deducts upfront processing fees, documentation charges, stamp duty, and applicable GST from the approved gross loan amount. For example, if you are approved for ₹5,00,000 and upfront fees total ₹15,000, your net disbursal will be ₹4,85,000.",
            keywords = listOf("net disbursal", "disbursed amount", "in-hand amount", "deductions", "gross vs net"),
            relatedQuestionIds = listOf("fees_processing_gst", "safety_sanction_letter", "loan_process"),
        ),
        FaqItem(
            id = "safety_loan_scams",
            category = FaqCategory.BANKING_SAFETY,
            question = "What are common loan scams?",
            answer = "Common loan scams in India include:\n• Illegal instant loan apps demanding access to your contacts or gallery.\n• Scammers demanding advance 'processing fees' or 'security deposits' to be transferred via UPI before loan approval.\n• Fake calls claiming to be from CIBIL or the RBI offering to 'erase' your negative credit history for money.\n• Lenders who do not disclose the parent RBI-registered NBFC or Bank. Legitimate lenders deduct fees from the loan disbursal, never upfront via personal UPI.",
            keywords = listOf("scams", "fraud", "illegal apps", "fake loan", "advance fee scam", "fake cibil"),
            relatedQuestionIds = listOf("safety_protect_credentials", "safety_verify_lender", "cibil_correct_errors"),
        ),
        FaqItem(
            id = "safety_protect_credentials",
            category = FaqCategory.BANKING_SAFETY,
            question = "Why should I never share my OTP, UPI PIN or bank password?",
            answer = "Your OTP (One-Time Password), UPI PIN, card CVV, and net banking password are confidential security credentials designed solely for you to authorize transactions. Bank staff, FinShield, and legitimate lenders will NEVER ask for these. Sharing them gives unauthorized persons complete control to drain your bank account or take fraudulent credit in your name.",
            keywords = listOf("otp", "pin", "password", "cvv", "never share", "security", "credentials", "safety warning"),
            relatedQuestionIds = listOf("safety_loan_scams", "safety_verify_lender", "finshield_aa_vs_cibil"),
        ),
        FaqItem(
            id = "safety_verify_lender",
            category = FaqCategory.BANKING_SAFETY,
            question = "How can I verify a lender before borrowing?",
            answer = "To verify an authorized lender:\n1. Check the RBI website (rbi.org.in) for the list of registered Scheduled Commercial Banks and RBI-registered NBFCs.\n2. Ensure digital lending apps clearly name their partner regulated bank/NBFC on their website and app store listing.\n3. Verify that the lender provides a Key Fact Statement (KFS) and sanction letter before signing.\n4. Avoid any entity that requires access to your private phone contacts or photos.",
            keywords = listOf("verify lender", "rbi registered", "nbfc list", "check bank", "legit lender", "safe borrowing"),
            relatedQuestionIds = listOf("safety_loan_scams", "safety_protect_credentials", "safety_sanction_letter"),
        ),

        // ==========================================
        // CATEGORY 5: FINSHIELD FEATURES
        // ==========================================
        FaqItem(
            id = "finshield_analyzer",
            category = FaqCategory.FINSHIELD_FEATURES,
            question = "How does Analyze Agreement work?",
            answer = "FinShield's Analyze Agreement feature uses on-device document extraction and OCR to read your loan agreement PDF or scanned pages. It verifies the mathematical calculations (reducing balance EMI, APR vs stated rate, total repayment) and audits contract clauses against consumer-protection standards to detect hidden charges, unfair default terms, and flat-rate traps.",
            keywords = listOf("analyze agreement", "how it works", "pdf scan", "ocr", "document review", "clause check"),
            relatedQuestionIds = listOf("finshield_risk_flag", "finshield_pdf_download", "interest_flat"),
        ),
        FaqItem(
            id = "finshield_risk_flag",
            category = FaqCategory.FINSHIELD_FEATURES,
            question = "What does a risk flag mean?",
            answer = "A risk flag in FinShield indicates a term or discrepancy in your loan agreement that could cost you money or limit your rights. Examples include:\n• Stated interest rate differs from the actual calculated EMI rate.\n• Disclosed flat rate without reducing balance equivalent.\n• Unusually high bounce charges or unilateral interest revision clauses.\nReview these flags before signing to negotiate or clarify with your lender.",
            keywords = listOf("risk flag", "warning", "red flag", "discrepancy", "agreement risk"),
            relatedQuestionIds = listOf("finshield_analyzer", "finshield_pdf_download", "interest_rate_vs_apr"),
        ),
        FaqItem(
            id = "finshield_pdf_download",
            category = FaqCategory.FINSHIELD_FEATURES,
            question = "How do I download my PDF report?",
            answer = "After analyzing an agreement, tap 'Download Report (PDF)' directly from the Analysis Report screen. FinShield generates an official, high-resolution multi-page PDF summary detailing extracted loan terms, risk findings, amortization breakdown, and mathematical verifications that you can save or share.",
            keywords = listOf("download pdf", "pdf report", "save report", "export", "share analysis"),
            relatedQuestionIds = listOf("finshield_analyzer", "finshield_risk_flag", "compare_loan_offers"),
        ),
        FaqItem(
            id = "finshield_eligibility",
            category = FaqCategory.FINSHIELD_FEATURES,
            question = "What does Estimated Loan Eligibility mean?",
            answer = "FinShield's Loan Eligibility calculator estimates your borrowing capacity based on your Fixed Obligation to Income Ratio (FOIR). Most Indian banks cap total monthly EMI commitments at 40%–50% of your net income. This feature helps you gauge whether a requested loan is comfortably affordable before you make a formal application.",
            keywords = listOf("eligibility", "borrowing capacity", "foir", "dti", "affordable", "calculator"),
            relatedQuestionIds = listOf("finshield_guarantee", "emi_calculation", "cibil_factors"),
        ),
        FaqItem(
            id = "finshield_guarantee",
            category = FaqCategory.FINSHIELD_FEATURES,
            question = "Can FinShield guarantee loan approval?",
            answer = "No. FinShield is an independent educational and document-verification tool, not a lender or credit broker. Loan approval, interest rates, and loan amounts are determined solely by the respective bank or NBFC based on their underwriting policies, internal risk assessment, and verification.",
            keywords = listOf("guarantee", "loan approval", "guaranteed loan", "broker", "lender approval"),
            relatedQuestionIds = listOf("finshield_eligibility", "cibil_rejection_reasons", "safety_verify_lender"),
        ),
        FaqItem(
            id = "finshield_aa_vs_cibil",
            category = FaqCategory.FINSHIELD_FEATURES,
            question = "Does connecting a bank account automatically provide an official CIBIL score?",
            answer = "No. Connecting your bank account uses the RBI Account Aggregator framework to share read-only bank transaction summaries for cash-flow assessment. An official CIBIL score comes exclusively from credit bureaus (TransUnion CIBIL) and requires your PAN, personal identification, and explicit consent under the Credit Information Companies Act.",
            keywords = listOf("bank account", "account aggregator", "cibil score difference", "aa vs cibil", "connect bank"),
            relatedQuestionIds = listOf("cibil_score_definition", "safety_bank_statement", "safety_protect_credentials"),
        ),
    )

    private val idToItemMap = items.associateBy { it.id }

    fun findById(id: String): FaqItem? = idToItemMap[id]

    fun getQuestionsForCategory(category: FaqCategory): List<FaqItem> =
        items.filter { it.category == category }

    fun getRelatedQuestions(item: FaqItem): List<String> =
        item.relatedQuestionIds.mapNotNull { idToItemMap[it]?.question }
}
