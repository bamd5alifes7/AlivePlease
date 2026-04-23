/**
 * Alive Please mail webhook for Google Apps Script.
 *
 * Expected POST body:
 * {
 *   "to": "family@example.com",
 *   "subject": "Alive Please check-in",
 *   "body": "message text"
 * }
 *
 * Optional:
 * - Add ?token=YOUR_SHARED_SECRET to the webhook URL and set WEBHOOK_TOKEN below.
 * - Update MAIL_FROM_NAME if you want a custom display name.
 */

const CONFIG = {
  WEBHOOK_TOKEN: '',
  MAIL_FROM_NAME: 'Alive Please',
  ALLOW_HTML: false,
  MAX_SUBJECT_LENGTH: 200,
  MAX_BODY_LENGTH: 20000,
  DEBUG_MODE: true,
  USAGE_LOG_SHEET_ID: '1gnj6ful_6zIg5EKLdrPVGFyHq2u115HWedrDGw32mPY',
  USAGE_LOG_SHEET_NAME: 'usage_log',
  LOG_HEALTHCHECKS: true,
};

function doGet(e) {
  const response = {
    ok: true,
    service: 'alive-please-webhook',
    message: 'Webhook is running.',
    time: new Date().toISOString(),
    hasToken: Boolean(CONFIG.WEBHOOK_TOKEN),
    query: e && e.parameter ? Object.keys(e.parameter) : [],
  };

  if (CONFIG.LOG_HEALTHCHECKS) {
    logUsageSafe_({
      event: 'healthcheck',
      ok: true,
      httpMethod: 'GET',
      tokenProvided: Boolean(e && e.parameter && e.parameter.token),
      queryKeys: response.query.join(','),
    });
  }

  return jsonOutput_(response);
}

function doPost(e) {
  try {
    assertAuthorized_(e);
    const payload = parsePayload_(e);
    const normalized = normalizePayload_(payload);

    const mailOptions = {
      name: CONFIG.MAIL_FROM_NAME,
    };

    if (CONFIG.ALLOW_HTML) {
      mailOptions.htmlBody = normalized.body;
    }

    MailApp.sendEmail(
      normalized.to,
      normalized.subject,
      CONFIG.ALLOW_HTML ? stripHtml_(normalized.body) : normalized.body,
      mailOptions
    );

    console.log(
      JSON.stringify({
        event: 'mail_sent',
        to: normalized.to,
        subjectLength: normalized.subject.length,
        bodyLength: normalized.body.length,
      })
    );

    logUsageSafe_({
      event: 'mail_sent',
      ok: true,
      httpMethod: 'POST',
      to: normalized.to,
      subjectLength: normalized.subject.length,
      bodyLength: normalized.body.length,
      tokenProvided: Boolean(e && e.parameter && e.parameter.token),
      postBodyLength: getPostBodyLength_(e),
    });

    return jsonOutput_({
      ok: true,
      message: 'Email sent.',
      to: normalized.to,
      time: new Date().toISOString(),
    });
  } catch (error) {
    const details = formatError_(error);
    console.error(details);
    logUsageSafe_({
      event: 'mail_error',
      ok: false,
      httpMethod: 'POST',
      tokenProvided: Boolean(e && e.parameter && e.parameter.token),
      postBodyLength: getPostBodyLength_(e),
      errorName: details.name,
      errorMessage: details.message,
    });
    return jsonOutput_({
      ok: false,
      error: details.message,
      name: details.name,
      stack: CONFIG.DEBUG_MODE ? details.stack : undefined,
      time: new Date().toISOString(),
    });
  }
}

function testSendEmail() {
  const recipient = Session.getActiveUser().getEmail() || Session.getEffectiveUser().getEmail();
  if (!recipient) {
    throw new Error('No active email found for testSendEmail().');
  }

  MailApp.sendEmail({
    to: recipient,
    subject: 'Alive Please GAS test',
    body: 'If you received this email, MailApp.sendEmail is working.',
    name: CONFIG.MAIL_FROM_NAME,
  });

  console.log(JSON.stringify({ event: 'test_mail_sent', to: recipient }));
  logUsageSafe_({
    event: 'test_mail_sent',
    ok: true,
    httpMethod: 'MANUAL',
    to: recipient,
  });
}

function assertAuthorized_(e) {
  if (!CONFIG.WEBHOOK_TOKEN) {
    return;
  }

  const token = e && e.parameter ? String(e.parameter.token || '') : '';
  if (token !== CONFIG.WEBHOOK_TOKEN) {
    throw new Error('Invalid token.');
  }
}

function parsePayload_(e) {
  if (!e || !e.postData || !e.postData.contents) {
    throw new Error('Missing request body.');
  }

  try {
    return JSON.parse(e.postData.contents);
  } catch (error) {
    throw new Error('Body must be valid JSON.');
  }
}

function normalizePayload_(payload) {
  if (!payload || typeof payload !== 'object') {
    throw new Error('Payload must be an object.');
  }

  const to = sanitizeText_(payload.to);
  const subject = sanitizeText_(payload.subject);
  const body = sanitizeBody_(payload.body);

  if (!to) {
    throw new Error('"to" is required.');
  }
  if (!subject) {
    throw new Error('"subject" is required.');
  }
  if (!body) {
    throw new Error('"body" is required.');
  }
  if (!isValidEmail_(to)) {
    throw new Error('Invalid recipient email.');
  }
  if (subject.length > CONFIG.MAX_SUBJECT_LENGTH) {
    throw new Error('Subject is too long.');
  }
  if (body.length > CONFIG.MAX_BODY_LENGTH) {
    throw new Error('Body is too long.');
  }

  return { to, subject, body };
}

function sanitizeText_(value) {
  return String(value == null ? '' : value)
    .replace(/\r\n/g, '\n')
    .replace(/\u0000/g, '')
    .trim();
}

function sanitizeBody_(value) {
  return String(value == null ? '' : value)
    .replace(/\r\n/g, '\n')
    .replace(/\u0000/g, '')
    .trim();
}

function stripHtml_(value) {
  return String(value)
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function isValidEmail_(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function jsonOutput_(data) {
  return ContentService
    .createTextOutput(JSON.stringify(data, null, 2))
    .setMimeType(ContentService.MimeType.JSON);
}

function logUsageSafe_(entry) {
  try {
    logUsage_(entry);
  } catch (error) {
    console.error(
      JSON.stringify({
        event: 'usage_log_failed',
        message: error && error.message ? String(error.message) : String(error),
      })
    );
  }
}

function logUsage_(entry) {
  if (!CONFIG.USAGE_LOG_SHEET_ID) {
    return;
  }

  const sheet = getUsageLogSheet_();
  ensureUsageLogHeader_(sheet);

  const timestamp = new Date();
  const scriptTimeZone = Session.getScriptTimeZone() || 'Etc/GMT';
  const payload = entry || {};

  sheet.appendRow([
    timestamp.toISOString(),
    Utilities.formatDate(timestamp, scriptTimeZone, 'yyyy-MM-dd HH:mm:ss'),
    payload.event || '',
    payload.ok === false ? 'false' : 'true',
    payload.httpMethod || '',
    payload.to || '',
    payload.subjectLength || '',
    payload.bodyLength || '',
    payload.postBodyLength || '',
    payload.tokenProvided === true ? 'true' : 'false',
    payload.queryKeys || '',
    payload.errorName || '',
    payload.errorMessage || '',
  ]);
}

function getUsageLogSheet_() {
  const spreadsheet = SpreadsheetApp.openById(CONFIG.USAGE_LOG_SHEET_ID);
  const existingSheet = spreadsheet.getSheetByName(CONFIG.USAGE_LOG_SHEET_NAME);
  if (existingSheet) {
    return existingSheet;
  }
  return spreadsheet.insertSheet(CONFIG.USAGE_LOG_SHEET_NAME);
}

function ensureUsageLogHeader_(sheet) {
  const headers = [
    'timestamp_iso',
    'timestamp_local',
    'event',
    'ok',
    'http_method',
    'recipient',
    'subject_length',
    'body_length',
    'post_body_length',
    'token_provided',
    'query_keys',
    'error_name',
    'error_message',
  ];

  if (sheet.getLastRow() > 0) {
    return;
  }

  sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
  sheet.setFrozenRows(1);
}

function getPostBodyLength_(e) {
  if (!e || !e.postData || !e.postData.contents) {
    return 0;
  }
  return String(e.postData.contents).length;
}

function formatError_(error) {
  return {
    name: error && error.name ? String(error.name) : 'Error',
    message: error && error.message ? String(error.message) : String(error),
    stack: error && error.stack ? String(error.stack) : '',
  };
}
