package org.delcom.ifs25042springbootcashflow.adapter;

import org.delcom.ifs25042springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25042springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25042springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25042springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25042springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiFinanceServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void validatesConfigAndHandlesNetworkAndHttpErrors() throws Exception {
        HttpClient client = mock(HttpClient.class);
        ObjectMapper mapper = new ObjectMapper();

        OpenAiFinanceService service = new OpenAiFinanceService("https://example.com/v1/", "secret", "", client, mapper);
        assertThatThrownBy(() -> service.generateTransactions(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.generateTransactions("  ")).isInstanceOf(ValidationException.class);

        OpenAiFinanceService noEndpoint = new OpenAiFinanceService("  ", "key", "m", client, mapper);
        assertThatThrownBy(() -> noEndpoint.generateTransactions("hi")).isInstanceOf(ValidationException.class);
        OpenAiFinanceService nullEndpoint = new OpenAiFinanceService(null, "key", "m", client, mapper);
        assertThatThrownBy(() -> nullEndpoint.generateTransactions("hi")).isInstanceOf(ValidationException.class);

        OpenAiFinanceService noKey = new OpenAiFinanceService("https://example.com/v1", " ", "m", client, mapper);
        assertThatThrownBy(() -> noKey.generateTransactions("hi")).isInstanceOf(ValidationException.class);
        OpenAiFinanceService nullKey = new OpenAiFinanceService("https://example.com/v1", null, "m", client, mapper);
        assertThatThrownBy(() -> nullKey.generateTransactions("hi")).isInstanceOf(ValidationException.class);

        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("timeout"));
        assertThatThrownBy(() -> service.generateTransactions("gaji"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Gagal menghubungi server AI");

        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));
        assertThatThrownBy(() -> service.generateTransactions("gaji"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("terinterupsi");
        assertThat(Thread.interrupted()).isTrue();

        HttpResponse<String> clientError = mock(HttpResponse.class);
        when(clientError.statusCode()).thenReturn(401);
        when(clientError.body()).thenReturn("Unauthorized");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(clientError);
        assertThatThrownBy(() -> service.generateTransactions("gaji"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("401");

        HttpResponse<String> serverError = mock(HttpResponse.class);
        when(serverError.statusCode()).thenReturn(500);
        when(serverError.body()).thenReturn("Internal Error");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(serverError);
        assertThatThrownBy(() -> service.generateTransactions("gaji"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("500");

        HttpResponse<String> okAfterRetry = mock(HttpResponse.class);
        when(okAfterRetry.statusCode()).thenReturn(200);
        when(okAfterRetry.body()).thenReturn(wrapChoice("[{\"description\":\"Bonus\",\"amount\":50000,\"type\":\"INCOME\"}]"));
        HttpClient retryClient = mock(HttpClient.class);
        when(retryClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(serverError, okAfterRetry);
        OpenAiFinanceService retryService = new OpenAiFinanceService("https://example.com/v1", "secret", "m", retryClient, mapper);
        assertThat(retryService.generateTransactions("bonus")).hasSize(1);
        verify(retryClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any())).thenThrow(new RuntimeException("serialize error"));
        OpenAiFinanceService brokenService = new OpenAiFinanceService(
                "https://example.com/v1/chat/completions", "k", null, client, brokenMapper
        );
        assertThatThrownBy(() -> brokenService.generateTransactions("gaji"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Gagal menyusun request AI");
    }

    @Test
    @SuppressWarnings("unchecked")
    void parsesValidAndInvalidAiResponses() throws Exception {
        HttpClient client = mock(HttpClient.class);
        OpenAiFinanceService service = new OpenAiFinanceService(
                "https://example.com/v1/chat/completions", "key", "gpt-4o-mini", client, new ObjectMapper()
        );

        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(resp);

        OpenAiFinanceService defaultClientService = new OpenAiFinanceService("https://example.com/v1", "secret", "m");
        assertThatThrownBy(() -> defaultClientService.generateTransactions(" "))
                .isInstanceOf(ValidationException.class);

        String validContent = """
                ```json
                [
                  {"amount": 1000, "type": "INCOME"},
                  {"description": "", "amount": 1000, "type": "INCOME"},
                  {"description": "No amount", "type": "INCOME"},
                  {"description": "Zero amount", "amount": 0, "type": "INCOME"},
                  {"description": "No type", "amount": 1000},
                  {"description": "Bad type", "amount": 1000, "type": "UNKNOWN"},
                  {"description": "Gaji", "amount": 5000000, "type": "INCOME"},
                  {"description": "Bonus", "amount": 500000, "type": "PEMASUKAN"},
                  {"description": "Kopi", "amount": 25000, "type": "EXPENSE"},
                  {"description": "Transport", "amount": 50000, "type": "PENGELUARAN"}
                ]
                ```""";
        when(resp.body()).thenReturn(wrapChoice(validContent));
        List<AiGeneratedTransaction> items = service.generateTransactions("test");
        assertThat(items).hasSize(4);
        assertThat(items.get(0)).isEqualTo(new AiGeneratedTransaction("Gaji", 5_000_000L, TransactionType.INCOME));
        assertThat(items.get(1).type()).isEqualTo(TransactionType.INCOME);
        assertThat(items.get(2).type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(items.get(3).type()).isEqualTo(TransactionType.EXPENSE);

        when(resp.body()).thenReturn(wrapChoice("```[{\"description\":\"Makan\",\"amount\":20000,\"type\":\"EXPENSE\"}]"));
        assertThat(service.generateTransactions("fence without newline")).hasSize(1);

        when(resp.body()).thenReturn("{}");
        assertThatThrownBy(() -> service.generateTransactions("x")).isInstanceOf(DomainException.class);

        when(resp.body()).thenReturn("{\"choices\":[]}");
        assertThatThrownBy(() -> service.generateTransactions("x")).isInstanceOf(DomainException.class);

        when(resp.body()).thenReturn("{\"choices\":[{}]}");
        assertThatThrownBy(() -> service.generateTransactions("x")).isInstanceOf(DomainException.class);

        when(resp.body()).thenReturn("{\"choices\":[{\"message\":{}}]}");
        assertThatThrownBy(() -> service.generateTransactions("x")).isInstanceOf(DomainException.class);

        when(resp.body()).thenReturn(wrapChoice("{\"not\":\"an array\"}"));
        assertThatThrownBy(() -> service.generateTransactions("x")).isInstanceOf(DomainException.class);

        when(resp.body()).thenReturn(wrapChoice("[]"));
        assertThatThrownBy(() -> service.generateTransactions("x")).isInstanceOf(DomainException.class);

        when(resp.body()).thenReturn("not-json");
        assertThatThrownBy(() -> service.generateTransactions("x")).isInstanceOf(DomainException.class);
    }

    private String wrapChoice(String content) throws Exception {
        return new ObjectMapper().writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("content", content))))
        );
    }
}
