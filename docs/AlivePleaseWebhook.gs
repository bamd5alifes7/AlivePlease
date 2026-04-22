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
};

function doGet(e) {
  return jsonOutput_({
    ok: true,
    service: 'alive-please-webhook',
    message: 'Webhook is running.',
    time: new Date().toISOString(),
    hasToken: Boolean(CONFIG.WEBHOOK_TOKEN),
    query: e && e.parameter ? Object.keys(e.parameter) : [],
  });
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

    return jsonOutput_({
      ok: true,
      message: 'Email sent.',
      to: normalized.to,
      time: new Date().toISOString(),
    });
  } catch (error) {
    const details = formatError_(error);
    console.error(details);
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

function formatError_(error) {
  return {
    name: error && error.name ? String(error.name) : 'Error',
    message: error && error.message ? String(error.message) : String(error),
    stack: error && error.stack ? String(error.stack) : '',
  };
}
